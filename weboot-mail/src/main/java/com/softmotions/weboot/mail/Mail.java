package com.softmotions.weboot.mail;

import java.util.Date;
import java.util.List;
import javax.mail.internet.InternetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jodd.mail.Email;
import jodd.mail.EmailAddress;
import jodd.mail.EmailAttachment;
import jodd.mail.EmailAttachmentBuilder;
import jodd.mail.MailException;
import jodd.mail.SendMailSession;
import jodd.mail.SmtpServer;

import com.google.inject.Provider;
import com.softmotions.weboot.executor.TaskExecutor;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class Mail extends Email {

    private static final Logger log = LoggerFactory.getLogger(Mail.class);

    private final Provider<SmtpServer> sendMailSessionProvider;

    private final Provider<TaskExecutor> taskExecutorProvider;

    private final boolean emulation;

    private final MailModule.MailServiceImpl service;

    public Mail(MailModule.MailServiceImpl service,
                Provider<SmtpServer> sendMailSessionProvider,
                Provider<TaskExecutor> taskExecutorProvider,
                boolean emulation) {
        this.sendMailSessionProvider = sendMailSessionProvider;
        this.taskExecutorProvider = taskExecutorProvider;
        this.emulation = emulation;
        this.service = service;
    }

    @Override
    public Mail from(String from) {
        return (Mail) super.from(from);
    }

    @Override
    public Mail from(String personal, String from) {
        return (Mail) super.from(personal, from);
    }

    @Override
    public Mail from(EmailAddress emailAddress) {
        return (Mail) super.from(emailAddress);
    }

    @Override
    public Mail from(InternetAddress internetAddress) {
        return (Mail) super.from(internetAddress);
    }

    @Override
    public Mail to(String to) {
        return (Mail) super.to(to);
    }

    @Override
    public Mail to(String personalName, String to) {
        return (Mail) super.to(personalName, to);
    }

    @Override
    public Mail to(EmailAddress emailAddress) {
        return (Mail) super.to(emailAddress);
    }

    @Override
    public Mail to(InternetAddress internetAddress) {
        return (Mail) super.to(internetAddress);
    }

    @Override
    public Mail to(String[] tos) {
        return (Mail) super.to(tos);
    }

    @Override
    public Mail to(EmailAddress[] tos) {
        return (Mail) super.to(tos);
    }

    @Override
    public Mail to(InternetAddress[] tos) {
        return (Mail) super.to(tos);
    }

    @Override
    public Mail replyTo(String replyTo) {
        return (Mail) super.replyTo(replyTo);
    }

    @Override
    public Mail replyTo(String personalName, String replyTo) {
        return (Mail) super.replyTo(personalName, replyTo);
    }

    @Override
    public Mail replyTo(EmailAddress emailAddress) {
        return (Mail) super.replyTo(emailAddress);
    }

    @Override
    public Mail replyTo(InternetAddress internetAddress) {
        return (Mail) super.replyTo(internetAddress);
    }

    @Override
    public Mail replyTo(String[] replyTos) {
        return (Mail) super.replyTo(replyTos);
    }

    @Override
    public Mail replyTo(EmailAddress[] replyTos) {
        return (Mail) super.replyTo(replyTos);
    }

    @Override
    public Mail replyTo(InternetAddress[] replyTos) {
        return (Mail) super.replyTo(replyTos);
    }

    @Override
    public Mail cc(String cc) {
        return (Mail) super.cc(cc);
    }

    @Override
    public Mail cc(String personalName, String cc) {
        return (Mail) super.cc(personalName, cc);
    }

    @Override
    public Mail cc(EmailAddress emailAddress) {
        return (Mail) super.cc(emailAddress);
    }

    @Override
    public Mail cc(InternetAddress internetAddress) {
        return (Mail) super.cc(internetAddress);
    }

    @Override
    public Mail cc(String[] ccs) {
        return (Mail) super.cc(ccs);
    }

    @Override
    public Mail cc(EmailAddress[] ccs) {
        return (Mail) super.cc(ccs);
    }

    @Override
    public Mail cc(InternetAddress[] ccs) {
        return (Mail) super.cc(ccs);
    }

    @Override
    public Mail bcc(String bcc) {
        return (Mail) super.bcc(bcc);
    }

    @Override
    public Mail bcc(String personal, String bcc) {
        return (Mail) super.bcc(personal, bcc);
    }

    @Override
    public Mail bcc(EmailAddress emailAddress) {
        return (Mail) super.bcc(emailAddress);
    }

    @Override
    public Mail bcc(InternetAddress internetAddress) {
        return (Mail) super.bcc(internetAddress);
    }

    @Override
    public Mail bcc(String[] bccs) {
        return (Mail) super.bcc(bccs);
    }

    @Override
    public Mail bcc(EmailAddress[] bccs) {
        return (Mail) super.bcc(bccs);
    }

    @Override
    public Mail bcc(InternetAddress[] bccs) {
        return (Mail) super.bcc(bccs);
    }

    @Override
    public Mail subject(String subject) {
        return (Mail) super.subject(subject);
    }

    @Override
    public Mail subject(String subject, String subjectEncoding) {
        return (Mail) super.subject(subject, subjectEncoding);
    }

    @Override
    public Mail message(String text, String mimeType, String encoding) {
        return (Mail) super.message(text, mimeType, encoding);
    }

    @Override
    public Mail message(String text, String mimeType) {
        return (Mail) super.message(text, mimeType);
    }

    @Override
    public Mail addText(String text) {
        return (Mail) super.addText(text);
    }

    @Override
    public Mail addText(String text, String encoding) {
        return (Mail) super.addText(text, encoding);
    }

    @Override
    public Mail addHtml(String message) {
        return (Mail) super.addHtml(message);
    }

    @Override
    public Mail addHtml(String message, String encoding) {
        return (Mail) super.addHtml(message, encoding);
    }

    @Override
    public List<EmailAttachment> getAttachments() {
        return super.getAttachments();
    }

    @Override
    public Mail attach(EmailAttachment emailAttachment) {
        return (Mail) super.attach(emailAttachment);
    }

    @Override
    public Mail embed(EmailAttachment emailAttachment) {
        return (Mail) super.embed(emailAttachment);
    }

    @Override
    public Mail attach(EmailAttachmentBuilder emailAttachmentBuilder) {
        return (Mail) super.attach(emailAttachmentBuilder);
    }

    @Override
    public Mail embed(EmailAttachmentBuilder emailAttachmentBuilder) {
        return (Mail) super.embed(emailAttachmentBuilder);
    }

    @Override
    public Mail header(String name, String value) {
        return (Mail) super.header(name, value);
    }

    @Override
    public Mail priority(int priority) {
        return (Mail) super.priority(priority);
    }

    @Override
    public Mail setCurrentSentDate() {
        return (Mail) super.setCurrentSentDate();
    }

    @Override
    public Mail sentOn(Date date) {
        return (Mail) super.sentOn(date);
    }

    public void send(String text) throws MailException {
        if (text != null) {
            addText(text, "UTF-8");
        }
        send();
    }

    public void send() throws MailException {
        log.info("Sending email: {}", this);
        service.onMailSent(this);
        if (emulation) {
            return;
        }
        SendMailSession session = sendMailSessionProvider.get().createSession();
        try {
            session.open();
            session.sendMail(this);
        } finally {
            session.close();
        }
    }

    public void sendAsync() {
        sendAsync(null);
    }

    public void sendAsync(String text) {
        if (emulation) {
            send(text);
            return;
        }
        TaskExecutor executor = taskExecutorProvider.get();
        executor.execute(() -> {
            try {
                send(text);
            } catch (MailException e) {
                log.warn(e.getMessage());
            }
        });
    }
}
