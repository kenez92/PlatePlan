package com.kenez92.plateplan.plan.db;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface SavedDishRepository extends JpaRepository<SavedDish, Long> {

    /**
     * Saved dishes of one login, oldest {@code dish_id} first.
     */
    List<SavedDish> findByLoginOrderByDishIdAsc(final String login);

    /**
     * Saved dishes of one login, newest {@code dish_id} first.
     */
    List<SavedDish> findByLoginOrderByDishIdDesc(final String login);

    /**
     * Removes these dishes of this login. Returns the number of matching rows. A dish of another
     * login matches nothing. The insert of the new dishes is flushed first, and the persistence
     * context is cleared so a deleted row is not written again on commit.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("delete from SavedDish dish where dish.dishId in :dishIds and dish.login = :login")
    int deleteByDishIdInAndLogin(@Param("dishIds") final Collection<Long> dishIds,
                                 @Param("login") final String login);
}
