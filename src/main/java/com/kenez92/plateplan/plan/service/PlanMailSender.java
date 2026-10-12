package com.kenez92.plateplan.plan.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * The only owner of {@link JavaMailSender}. It attaches the two in-memory PDFs and does not log
 * the recipient, the sending account, or the bytes.
 */
@Service
public class PlanMailSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanMailSender.class);

    private static final String FROM_PROPERTY = "${spring.mail.username:}";
    private static final String FAILED_LOG = "Plan mail failed: {} (cause: {})";
    private static final String FROM_MISSING_LOG = "Plan mail failed: missing sending address";
    private static final String SUBJECT = "Dieta na jutro i lista zakupów";
    private static final String BODY = "W załączniku jadłospis na jutro i lista zakupów.";
    private static final String DIET_ATTACHMENT = "dieta-na-jutro.pdf";
    private static final String LIST_ATTACHMENT = "lista-zakupow.pdf";
    private static final String HTML_NOT_USED = "utf-8";

    private final JavaMailSender mailSender;
    private final String from;

    public PlanMailSender(final JavaMailSender mailSender, @Value(FROM_PROPERTY) final String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public boolean send(final String to, final byte[] dietPdf, final byte[] shoppingListPdf) {
        if (from.isBlank()) {
            LOGGER.warn(FROM_MISSING_LOG);
            return false;
        }
        try {
            final MimeMessage message = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(message, true, HTML_NOT_USED);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(SUBJECT);
            helper.setText(BODY);
            helper.addAttachment(DIET_ATTACHMENT, new ByteArrayResource(dietPdf));
            helper.addAttachment(LIST_ATTACHMENT, new ByteArrayResource(shoppingListPdf));
            mailSender.send(message);
            return true;
        } catch (final Exception exception) {
            LOGGER.warn(FAILED_LOG, exception.getClass().getName(),
                    NestedExceptionUtils.getMostSpecificCause(exception).getClass().getName());
            return false;
        }
    }
}
