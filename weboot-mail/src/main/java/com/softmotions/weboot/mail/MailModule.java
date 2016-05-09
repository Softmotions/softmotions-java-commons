package com.softmotions.weboot.mail;

import java.util.Collections;
import java.util.List;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
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
import com.softmotions.weboot.executor.TaskExecutor;

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
        XMLConfiguration xcfg = cfg.xcfg();
        SubnodeConfiguration mailCfg = xcfg.configurationAt("mail");
        if (mailCfg == null) {
            log.warn("No <mail> configuration found");
            return;
        }
        String host = mailCfg.getString("smtp.server");
        if (host == null) {
            throw new RuntimeException("No smtp server configured, check the <mail> configuration");
        }
        int port = mailCfg.getInt("smtp.port", 25);
        smtpServer = SmtpServer.create(host, port);
        if (!StringUtils.isBlank(mailCfg.getString("smtp.user"))) {
            smtpServer.authenticateWith(StringUtils.trim(mailCfg.getString("smtp.user")),
                                        StringUtils.trim(mailCfg.getString("smtp.password")));
        }

        bind(SmtpServer.class).annotatedWith(Names.named("com.softmotions.weboot.mail.MailModule")).toInstance(smtpServer);
        bind(MailService.class).to(MailServiceImpl.class).asEagerSingleton();
    }


    static class MailServiceImpl implements MailService {

        final XMLConfiguration xcfg;

        final Provider<TaskExecutor> executorProvider;

        final SmtpServer smtpServer;

        final Stack<Mail> history = new Stack<>();

        final Object lock = new Object();

        @Inject
        MailServiceImpl(XMLConfiguration xcfg,
                        Provider<TaskExecutor> executorProvider,
                        @Named("com.softmotions.weboot.mail.MailModule")
                        SmtpServer smtpServer) {
            this.xcfg = xcfg;
            this.executorProvider = executorProvider;
            this.smtpServer = smtpServer;
        }

        void onMailSent(Mail mail) {
            int numHist = xcfg.getInt("mail.keep-history", 0);
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
            return smtpServer.createSession();
        }

        @Override
        public Mail newMail() {
            Mail mail = new Mail(this,
                                 smtpServer, executorProvider,
                                 xcfg.getBoolean("mail.emulation", false));
            String val = xcfg.getString("mail.from");
            if (!StringUtils.isBlank(val)) {
                mail.from(val);
            }
            val = xcfg.getString("mail.subject");
            if (!StringUtils.isBlank(val)) {
                mail.subject(val);
            }
            val = xcfg.getString("mail.bcc");
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
