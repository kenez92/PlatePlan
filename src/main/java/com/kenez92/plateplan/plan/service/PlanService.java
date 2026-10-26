package com.kenez92.plateplan.plan.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.kenez92.plateplan.plan.model.PlanOption;
import com.kenez92.plateplan.plan.model.PlanPerson;
import com.kenez92.plateplan.plan.model.PlanResult;
import com.kenez92.plateplan.profile.db.HouseholdMember;
import com.kenez92.plateplan.profile.db.HouseholdMemberRepository;
import com.kenez92.plateplan.profile.db.UserProfile;
import com.kenez92.plateplan.profile.db.UserProfileRepository;
import com.kenez92.plateplan.profile.format.ProductListFormat;
import com.kenez92.plateplan.profile.model.Goal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

/**
 * Loads the owner and the added people, keeps the selected ones, and asks {@link DietGenerator}
 * once. No owner row is {@link PlanResult#noProfile()}. An empty selection is
 * {@link PlanResult#noSelection()} and does not read the database. A selected person with no
 * calorie target is {@link PlanResult#noCalories()}. An unselected person adds no portion, no
 * product, and no calorie failure. A null selection is everyone saved on the account. An empty
 * selection, or a token that matches nobody on the account, is {@link PlanResult#noSelection()}.
 * A database failure is {@link PlanResult#unavailable()}. Names, calories, goals, product lists,
 * and the delivery email are not logged.
 */
@Service
public class PlanService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanService.class);

    private static final String FAILED_LOG = "Plan load failed on the database: {} (cause: {})";
    private static final String OWNER = "owner";
    private static final String UNNAMED_OWNER = "Ja";

    private final UserProfileRepository userProfileRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final ProductListFormat productListFormat;
    private final DietGenerator dietGenerator;

    public PlanService(final UserProfileRepository userProfileRepository,
                       final HouseholdMemberRepository householdMemberRepository,
                       final ProductListFormat productListFormat,
                       final DietGenerator dietGenerator) {
        this.userProfileRepository = userProfileRepository;
        this.householdMemberRepository = householdMemberRepository;
        this.productListFormat = productListFormat;
        this.dietGenerator = dietGenerator;
    }

    /**
     * The owner, then members by {@code member_id}, as the plan screen lists them. A missing row or
     * a database failure is an empty list. The delivery email is not included.
     */
    public List<PlanOption> options(final String login) {
        try {
            return userProfileRepository.findById(login)
                    .map((final UserProfile owner) -> listed(
                            owner, householdMemberRepository.findByLoginOrderByMemberIdAsc(login)))
                    .orElseGet(List::of);
        } catch (final DataAccessException | TransactionException exception) {
            logFailure(exception);
            return List.of();
        }
    }

    /**
     * Cooks for the selected people, owner first and then {@code member_id} ascending. A null
     * selection is everyone saved on the account. {@code owner} selects the owner. Any other token
     * is a member id of this login. An empty selection, or a token that matches nobody on the
     * account, is {@link PlanResult#noSelection()} and does not call the model.
     */
    public PlanResult generate(final String login, final List<String> selected) {
        if (selected != null && selected.isEmpty()) {
            return PlanResult.noSelection();
        }
        try {
            return userProfileRepository.findById(login)
                    .map((final UserProfile owner) -> fromHousehold(login, owner, selected))
                    .orElseGet(PlanResult::noProfile);
        } catch (final DataAccessException | TransactionException exception) {
            logFailure(exception);
            return PlanResult.unavailable();
        }
    }

    private List<PlanOption> listed(final UserProfile owner, final List<HouseholdMember> members) {
        return Stream.concat(
                        Stream.of(new PlanOption(OWNER, effectiveName(owner.getDisplayName()))),
                        members.stream().map(this::memberOption))
                .toList();
    }

    private PlanOption memberOption(final HouseholdMember member) {
        return new PlanOption(Long.toString(member.getMemberId()), effectiveName(member.getDisplayName()));
    }

    private PlanResult fromHousehold(final String login, final UserProfile owner, final List<String> selected) {
        final List<HouseholdMember> members = householdMemberRepository.findByLoginOrderByMemberIdAsc(login);
        final List<StoredPerson> people = chosen(owner, members, selected == null ? everyKey(members) : selected);
        if (people.isEmpty()) {
            return PlanResult.noSelection();
        }
        if (people.stream().anyMatch((final StoredPerson person) -> person.kcal() == null)) {
            return PlanResult.noCalories();
        }
        return dietGenerator.generate(planPeople(people), preferred(people), union(people, StoredPerson::excluded));
    }

    private List<PlanPerson> planPeople(final List<StoredPerson> people) {
        return people.stream().map(this::planPerson).toList();
    }

    private PlanPerson planPerson(final StoredPerson person) {
        return new PlanPerson(person.name(), person.goal(), person.kcal());
    }

    private List<String> everyKey(final List<HouseholdMember> members) {
        return Stream.concat(
                        Stream.of(OWNER),
                        members.stream().map((final HouseholdMember member) -> Long.toString(member.getMemberId())))
                .toList();
    }

    private List<StoredPerson> chosen(final UserProfile owner,
                                      final List<HouseholdMember> members,
                                      final List<String> selected) {
        final List<StoredPerson> household = Stream.concat(
                        Stream.of(fromOwner(owner)), members.stream().map(this::fromMember))
                .toList();
        if (selected.stream().anyMatch((final String token) -> unknown(token, household))) {
            return List.of();
        }
        return household.stream()
                .filter((final StoredPerson person) -> selected.contains(person.key()))
                .toList();
    }

    private boolean unknown(final String token, final List<StoredPerson> household) {
        return household.stream().noneMatch((final StoredPerson person) -> person.key().equals(token));
    }

    private StoredPerson fromOwner(final UserProfile owner) {
        return new StoredPerson(
                OWNER,
                effectiveName(owner.getDisplayName()),
                owner.getGoal(),
                owner.getConfirmedCalories(),
                productListFormat.split(owner.getPreferredProducts()),
                productListFormat.split(owner.getExcludedProducts()));
    }

    private StoredPerson fromMember(final HouseholdMember member) {
        return new StoredPerson(
                Long.toString(member.getMemberId()),
                effectiveName(member.getDisplayName()),
                member.getGoal(),
                member.getConfirmedCalories(),
                productListFormat.split(member.getPreferredProducts()),
                productListFormat.split(member.getExcludedProducts()));
    }

    private String effectiveName(final String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return UNNAMED_OWNER;
        }
        return displayName.strip();
    }

    private List<String> preferred(final List<StoredPerson> people) {
        return without(union(people, StoredPerson::preferred), keys(union(people, StoredPerson::excluded)));
    }

    private List<String> without(final List<String> names, final Set<String> excludedKeys) {
        return names.stream()
                .filter((final String name) -> !excludedKeys.contains(productKey(name)))
                .toList();
    }

    private Set<String> keys(final List<String> names) {
        return names.stream().map(this::productKey).collect(Collectors.toSet());
    }

    private List<String> union(final List<StoredPerson> people, final Function<StoredPerson, List<String>> names) {
        return people.stream()
                .map(names)
                .flatMap(List::stream)
                .collect(Collectors.toMap(
                        this::productKey,
                        (final String name) -> name,
                        (final String first, final String ignored) -> first,
                        LinkedHashMap::new))
                .values()
                .stream()
                .toList();
    }

    private String productKey(final String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }

    private void logFailure(final Exception exception) {
        LOGGER.warn(FAILED_LOG, exception.getClass().getName(), causeName(exception));
    }

    private String causeName(final Exception exception) {
        return NestedExceptionUtils.getMostSpecificCause(exception).getClass().getName();
    }

    private record StoredPerson(String key,
                                String name,
                                Goal goal,
                                Integer kcal,
                                List<String> preferred,
                                List<String> excluded) {
    }
}
