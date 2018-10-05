package com.softmotions.weboot.mail;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jodd.mail.SendMailSession;
import jodd.mail.SmtpServer;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.softmotions.commons.ServicesConfiguration;
import com.softmotions.commons.cont.Stack;
import com.softmotions.xconfig.XConfig;

/**
 * Mail service module.
 * This module requires task executor module {@link TaskExecutorModule}
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class MailModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(MailModule.class);

    private final ServicesConfiguration cfg;

    SmtpServer smtpServer;

    public MailModule(ServicesConfiguration cfg) {
        this.cfg = cfg;
    }

    @Override
    protected void configure() {
        if (!cfg.xcfg().hasPattern("mail")) {
            log.warn("No <mail> configuration found");
            return;
        }
        var mailCfg = cfg.xcfg().subPattern("mail").get(0);
        String host = mailCfg.text("smtp.server");
        if (host == null) {
            throw new RuntimeException("No smtp server configured, check the <mail> configuration");
        }
        int port = mailCfg.numberPattern("smtp.port", 25L).intValue();
        smtpServer = SmtpServer.create(host, port);
        if (!StringUtils.isBlank(mailCfg.text("smtp.user"))) {
            smtpServer.authenticateWith(StringUtils.trim(mailCfg.text("smtp.user")),
                                        StringUtils.trim(mailCfg.text("smtp.password")));
        }

        bind(SmtpServer.class).annotatedWith(Names.named("com.softmotions.weboot.mail.MailModule")).toInstance(smtpServer);
        bind(MailService.class).to(MailServiceImpl.class).asEagerSingleton();
    }


    static class MailServiceImpl implements MailService {

        final XConfig xcfg;

        final Executor executor;

        final Provider<SmtpServer> smtpServer;

        final Stack<Mail> history = new Stack<>();

        final Object lock = new Object();

        @Inject
        MailServiceImpl(XConfig xcfg,
                        Executor executor,
                        @Named("com.softmotions.weboot.mail.MailModule") Provider<SmtpServer> smtpServer) {
            this.xcfg = xcfg;
            this.executor = executor;
            this.smtpServer = smtpServer;
        }

        void onMailSent(Mail mail) {
            int numHist = xcfg.numberPattern("mail.keep-history", 0L).intValue();
            if (numHist < 1) {
                return;
            }
            synchronized (lock) {
                while (history.size() > numHist) {
                    history.remove(0);
                }
                history.push(mail);
            }
        }

        @Override
        public SendMailSession createSession() {
            return smtpServer.get().createSession();
        }

        @Override
        public Mail newMail() {
            Mail mail = new Mail(this,
                                 smtpServer, executor,
                                 xcfg.boolPattern("mail.emulation", false));
            String val = xcfg.text("mail.from");
            if (!StringUtils.isBlank(val)) {
                mail.from(val);
            }
            val = xcfg.text("mail.subject");
            if (!StringUtils.isBlank(val)) {
                mail.subject(val);
            }
            val = xcfg.text("mail.bcc");
            if (!StringUtils.isBlank(val)) {
                mail.bcc(val);
            }
            return mail;
        }

        @Override
        public List<Mail> getHistory() {
            synchronized (lock) {
                return Collections.unmodifiableList(history);
            }
        }
    }
}
