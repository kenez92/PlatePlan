package com.kenez92.plateplan.profile.model;

import java.util.List;

import com.kenez92.plateplan.profile.db.HouseholdMember;

/**
 * The outcome of reading, storing, or removing an added person. A refusal carries the reason and
 * none of the stored values. Expected outcomes are values, not exceptions.
 */
public record HouseholdMemberResult(Status status,
                                    HouseholdMember member,
                                    List<HouseholdMember> members,
                                    HouseholdMemberError error) {

    /**
     * Which call succeeded, or that it was refused.
     */
    public enum Status {
        SAVED,
        LISTED,
        DELETED,
        REJECTED
    }

    public HouseholdMemberResult {
        members = List.copyOf(members);
    }

    public static HouseholdMemberResult saved(final HouseholdMember member) {
        return new HouseholdMemberResult(Status.SAVED, member, List.of(), null);
    }

    public static HouseholdMemberResult listed(final List<HouseholdMember> members) {
        return new HouseholdMemberResult(Status.LISTED, null, members, null);
    }

    public static HouseholdMemberResult deleted() {
        return new HouseholdMemberResult(Status.DELETED, null, List.of(), null);
    }

    public static HouseholdMemberResult rejected(final HouseholdMemberError error) {
        return new HouseholdMemberResult(Status.REJECTED, null, List.of(), error);
    }
}
