package ua.com.kerriline.location;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

/**
 * http://www.technicalkeeda.com/java/how-to-access-gmail-inbox-using-java-imap
 * 
 * @author Aleksey
 *
 */
@Component
public class MailManager {
	
	private static final Log LOG = LogFactory.getLog(MailManager.class);
	
	//TODO move to configuration
	private String email = "sledline@gmail.com";
	private String pass = "k5666031";

	public MessageBean getLast1392() {
		Properties props = new Properties();
		Message[] messages = null;
		MessageBean bean = null;
		try {
			props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("mail.properties"));
			Session session = Session.getDefaultInstance(props, null);

			Store store = session.getStore("imaps");
			
			
			store.connect("smtp.gmail.com", email, pass);

			Folder inbox = store.getFolder("inbox");
			inbox.open(Folder.READ_ONLY);
			int messageCount = inbox.getMessageCount();

			LOG.info("Total Messages:- " + messageCount);

			messages = inbox.getMessages();
			LOG.info("------------------------------");
			for (int i = messageCount; i > 0; i--) {
				LOG.info("Mail Subject:- " + messages[i].getSubject());
				if(messages[i].getSubject().contains("1392")){
					LOG.info("Found required mail");
					bean = new MessageBean(messages[i].getSubject(), getText(messages[i]), messages[i].getReceivedDate());
					break;
				}
			}
			inbox.close(true);
			store.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		
		return bean;
	}
	
	
	public List<MessageBean> getAll1392Messages() {
		Properties props = new Properties();
		Message[] messages = null;
		MessageBean bean = null;
		
		List<MessageBean> result = new ArrayList<MessageBean>();
		try {
			props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("mail.properties"));
			Session session = Session.getDefaultInstance(props, null);

			Store store = session.getStore("imaps");
			
			
			store.connect("smtp.gmail.com", email, pass);

			Folder inbox = store.getFolder("inbox");
			inbox.open(Folder.READ_ONLY);
			int messageCount = inbox.getMessageCount();

			LOG.info("Total Messages:- " + messageCount);
			
			messages = inbox.getMessages();
			
			LOG.info("------------------------------");
			
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.HOUR, -2);
			
			for (int i = 0; i < messageCount; i++) {
				LOG.info("Mail Subject:- " + messages[i].getSubject() + ", received " + messages[i].getReceivedDate());
				if(messages[i].getSubject().contains("1392") && cal.getTime().before(messages[i].getReceivedDate())){
					LOG.info("Found required mail");

					bean = new MessageBean(messages[i].getSubject(), getText(messages[i]), messages[i].getReceivedDate());
					result.add(bean);
				}
			}
			inbox.close(true);
			store.close();
		} catch (Exception e) {
			LOG.error("Failed read mails", e);
		}
		return result;
	}

    
    /**
     * Return the primary text content of the message.
     */
    private String getText(Part p) throws
                MessagingException, IOException {
        if (p.isMimeType("text/*")) {
            String s = (String)p.getContent();
            return s;
        }

        if (p.isMimeType("multipart/alternative")) {
            // prefer html text over plain text
            Multipart mp = (Multipart)p.getContent();
            String text = null;
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    if (text == null)
                        text = getText(bp);
                    continue;
                } else if (bp.isMimeType("text/html")) {
                    String s = getText(bp);
                    if (s != null)
                        return s;
                } else {
                    return getText(bp);
                }
            }
            return text;
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart)p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String s = getText(mp.getBodyPart(i));
                if (s != null)
                    return s;
            }
        }

        return null;
    }
}
