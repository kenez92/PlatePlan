package com.kenez92.plateplan.plan.db;

import com.kenez92.plateplan.plan.model.DishOpinion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One dish name of an account, with the preparation from the first time it was saved. The login is
 * stored as the account's login and is not a foreign key. {@code opinion} is null until the account
 * marks the dish. An older dish has a smaller {@code dish_id}.
 */
@Entity
@Table(name = "saved_dish")
public class SavedDish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dish_id", nullable = false)
    private Long dishId;

    @Column(name = "login", nullable = false)
    private String login;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "preparation", nullable = false)
    private String preparation;

    @Enumerated(EnumType.STRING)
    @Column(name = "opinion")
    private DishOpinion opinion;

    protected SavedDish() {
    }

    public SavedDish(final String login, final String name, final String preparation) {
        this(null, login, name, preparation, null);
    }

    public SavedDish(final Long dishId,
                     final String login,
                     final String name,
                     final String preparation,
                     final DishOpinion opinion) {
        this.dishId = dishId;
        this.login = login;
        this.name = name;
        this.preparation = preparation;
        this.opinion = opinion;
    }

    public Long getDishId() {
        return dishId;
    }

    public String getLogin() {
        return login;
    }

    public String getName() {
        return name;
    }

    public String getPreparation() {
        return preparation;
    }

    public DishOpinion getOpinion() {
        return opinion;
    }

    public void setOpinion(final DishOpinion opinion) {
        this.opinion = opinion;
    }
}
