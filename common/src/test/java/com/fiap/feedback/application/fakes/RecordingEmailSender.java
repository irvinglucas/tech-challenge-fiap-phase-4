package com.fiap.feedback.application.fakes;

import com.fiap.feedback.application.port.out.EmailSender;

import java.util.ArrayList;
import java.util.List;

public final class RecordingEmailSender implements EmailSender {

    public record SentEmail(String subject, String htmlBody, String dedupeKey) {
    }

    private final List<SentEmail> sent = new ArrayList<>();

    @Override
    public void send(String subject, String htmlBody, String dedupeKey) {
        sent.add(new SentEmail(subject, htmlBody, dedupeKey));
    }

    public List<SentEmail> sent() {
        return List.copyOf(sent);
    }
}
