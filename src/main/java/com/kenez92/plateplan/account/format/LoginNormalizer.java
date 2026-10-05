package com.kenez92.plateplan.account.format;

import java.text.Normalizer;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Brings a login to the one form that is stored and looked up: surrounding white space removed and
 * Unicode composed (NFC), so the same visible login always compares equal. Registration and sign-in
 * both use it. A missing login becomes an empty one, which the validator rejects.
 */
@Component
public class LoginNormalizer {

    public String normalize(final String login) {
        return Optional.ofNullable(login)
                .map(String::strip)
                .map(text -> Normalizer.normalize(text, Normalizer.Form.NFC))
                .orElse(StringUtils.EMPTY);
    }
}
