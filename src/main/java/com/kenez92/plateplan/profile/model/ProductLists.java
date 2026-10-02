package com.kenez92.plateplan.profile.model;

import java.util.List;

/**
 * The preferred and the excluded product names of a valid profile, in the order the user typed them.
 * Both lists are immutable copies.
 */
public record ProductLists(List<String> preferred, List<String> excluded) {

    public ProductLists {
        preferred = List.copyOf(preferred);
        excluded = List.copyOf(excluded);
    }
}
