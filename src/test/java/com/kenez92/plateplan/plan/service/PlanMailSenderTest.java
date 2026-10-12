package com.kenez92.plateplan.plan.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.stream.IntStream;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.mail.BodyPart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanMailSenderTest {

    @Test
    void shouldNotCallTransportWhenTheSendingAddressIsBlank() {
        final JavaMailSender mailSender = mock(JavaMailSender.class);
        final PlanMailSender sender = new PlanMailSender(mailSender, " ");

        final boolean actual = sender.send("alice@example.com", pdf("diet"), pdf("list"));

        assertThat(actual).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void shouldSendAMessageWithBothPdfAttachments() throws Exception {
        final JavaMailSender mailSender = mock(JavaMailSender.class);
        final MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        final PlanMailSender sender = new PlanMailSender(mailSender, "plateplan@example.com");

        final boolean actual = sender.send("alice@example.com", pdf("diet"), pdf("list"));

        assertThat(actual).isTrue();
        verify(mailSender).send(message);
        assertThat(message.getSubject()).isEqualTo("Dieta na 7 dni i lista zakupów");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("alice@example.com");
        final MimeMultipart multipart = (MimeMultipart) message.getContent();
        assertThat(text(multipart.getBodyPart(0).getContent()))
                .isEqualTo("W załączniku jadłospis na 7 dni i lista zakupów.");
        assertThat(attachmentNames(multipart)).containsExactly("dieta-na-tydzien.pdf", "lista-zakupow.pdf");
    }

    @Test
    void shouldReturnFalseAndLogOnlyClassNamesWhenTransportFails() {
        final JavaMailSender mailSender = mock(JavaMailSender.class);
        final MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("alice@example.com rejected")).when(mailSender).send(any(MimeMessage.class));
        final PlanMailSender sender = new PlanMailSender(mailSender, "plateplan@example.com");
        final Logger logger = (Logger) LoggerFactory.getLogger(PlanMailSender.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        final boolean actual;
        try {
            actual = sender.send("alice@example.com", pdf("diet"), pdf("list"));
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(actual).isFalse();
        final String log = appender.list.get(0).getFormattedMessage();
        assertThat(log).contains(MailSendException.class.getName());
        assertThat(log).doesNotContain("alice@example.com", "rejected");
    }

    private String text(final Object content) throws Exception {
        if (content instanceof MimeMultipart multipart) {
            return text(multipart.getBodyPart(0).getContent());
        }
        return content.toString();
    }

    private byte[] pdf(final String label) {
        return ("%PDF-1.4 " + label).getBytes(StandardCharsets.ISO_8859_1);
    }

    private List<String> attachmentNames(final MimeMultipart multipart) throws Exception {
        return IntStream.range(0, multipart.getCount())
                .mapToObj(index -> fileName(multipart, index))
                .filter(name -> name != null)
                .toList();
    }

    private String fileName(final MimeMultipart multipart, final int index) {
        try {
            final BodyPart part = multipart.getBodyPart(index);
            return part.getFileName();
        } catch (final Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
