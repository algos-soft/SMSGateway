package it.algos.smsgateway.mail;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import it.algos.smsgateway.AppContainer;
import it.algos.smsgateway.Constants;
import it.algos.smsgateway.R;
import it.algos.smsgateway.SmsGatewayApp;
import it.algos.smsgateway.services.PrefsService;

public class GMailService extends javax.mail.Authenticator {

    private Context context;
    private ExecutorService executorService = Executors.newFixedThreadPool(4);
    private String mailhost = "smtp.gmail.com";
    private Session session;

    static {
        Security.addProvider(new JSSEProvider());
    }

    public GMailService(Context context) {

        this.context=context;

        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.host", mailhost);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.setProperty("mail.smtp.quitwait", "false");

        session = Session.getDefaultInstance(props, this);
    }

    protected PasswordAuthentication getPasswordAuthentication() {

        String user = getPrefsService().getString(R.string.gmail_user);
        String password = getPrefsService().getString(R.string.gmail_password);
        if(TextUtils.isEmpty(user) || TextUtils.isEmpty(password)){
            throw new RuntimeException("empty GMail credentials - to send emails, set the GMail properties in app Settings");
        }

        return new PasswordAuthentication(user, password);
    }

    public synchronized void sendMail(String subject, String body) {

        String sender = getPrefsService().getString(R.string.gmail_user);
        String recipients = getPrefsService().getString(R.string.gmail_recipient);
        if(TextUtils.isEmpty(sender) || TextUtils.isEmpty(recipients)){
            throw new RuntimeException("empty GMail sender or recipient - to send emails, set the GMail properties in app Settings");
        }

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    sendMail(subject, body, sender, recipients);
                } catch (Exception e) {
                    Log.e(Constants.LOG_TAG,"Could not send email. subject="+subject+", body="+body, e);
                }
            }
        });

    }

    private synchronized void sendMail(String subject, String body, String sender, String recipients) throws Exception {
        MimeMessage message = new MimeMessage(session);
        DataHandler handler = new DataHandler(new ByteArrayDataSource(body.getBytes(), "text/plain"));
        message.setSender(new InternetAddress(sender));
        message.setSubject(subject);
        message.setDataHandler(handler);
        if (recipients.indexOf(',') > 0)
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
        else
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipients));
        Transport.send(message);
    }


    public class ByteArrayDataSource implements DataSource {
        private byte[] data;
        private String type;

        public ByteArrayDataSource(byte[] data, String type) {
            super();
            this.data = data;
            this.type = type;
        }

        public ByteArrayDataSource(byte[] data) {
            super();
            this.data = data;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getContentType() {
            if (type == null)
                return "application/octet-stream";
            else
                return type;
        }

        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }

        public String getName() {
            return "ByteArrayDataSource";
        }

        public OutputStream getOutputStream() throws IOException {
            throw new IOException("Not Supported");
        }
    }

    public PrefsService getPrefsService() {
        AppContainer appContainer = ((SmsGatewayApp) context).appContainer;
        return appContainer.getPrefsService();
    }


}
