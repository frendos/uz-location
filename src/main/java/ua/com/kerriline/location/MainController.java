package ua.com.kerriline.location;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@EnableAutoConfiguration
@Import({Config.class, MvcConfiguration.class})
public class MainController {

	private static final Log LOG = LogFactory.getLog(MainController.class);


	public static void main(String[] args) throws Exception {
		SpringApplication.run(MainController.class, args);
	}
	
	//@Inject	SchedulerManager scheduler;
	@Inject	MailManager mail;
	@Inject	MailParser source;
	@Inject	GDocsSheet sheet;
	
	@RequestMapping("/")
	@ResponseBody
	String home() {
		return "Hello World!";
	}

	@RequestMapping("/mail")
	@ResponseBody
	String mail() {
		try {
			mail.getAll1392Messages();
			return "done";
		} catch (Exception e) {
			LOG.error("Failed to get mails", e);
			return "failed";
		}
		
	}
	
	
	@RequestMapping("/send")
	@ResponseBody
	String send() {
		try {
			LOG.info("Authorizing");
			sheet.authorize();
			LOG.info("Reading tanks");
			List<Map<String, String>> tanks = sheet.readTanks();
			StringBuilder text = new StringBuilder();
			int i = 0;
			for (Map<String, String> tank : tanks) {
				i++;
				text.append(tank.get("вагон")).append("\n");
				if(i > 150) {
					LOG.info("Sending mail");
					mail.sendMail(text.toString());
					text.setLength(0);
					i = 0;
				}
			}
			LOG.info("Sending mail");
			mail.sendMail(text.toString());
			return "done";
		} catch (Exception e) {
			LOG.error("Failed to get mails", e);
			return "failed";
		}
		
	}
	
	
	@RequestMapping("/table")
	@ResponseBody
	String table() {
		return source.text2table(mail.getLast1392()).toString();
	}
	
	@RequestMapping("/sheet")
	@ResponseBody
	String sheet() {
		try {
			LOG.info("Authorizing");
			sheet.authorize();
			LOG.info("Reading tanks");
			List<Map<String, String>> tanks = sheet.readTanks();
			LOG.info("Reading column association");
			Map<String, String> columns = sheet.readColumns();
			LOG.info("Reading mails");
			List<MessageBean> messages = mail.getAll1392Messages();
			Collections.reverse(messages);
			for (MessageBean messageBean : messages) {
				List<Map<String, String>> rawData = source.text2table(messageBean);
				LOG.info("Merging tanks and mail data");
				List<Map<String, String>> newData = source.merge(tanks, rawData);
				LOG.info("Writing data");
				sheet.writeData(columns, newData);
			}
			LOG.info("Sheet updated");
			return "done";
		} catch (Exception e) {
			LOG.error("Failed to make sheet", e);
			return "failed";
		}
		
		
	}
	
	
	@RequestMapping("/full")
	@ResponseBody
	String full() {
		try {
			send();
			LOG.info("Sleep for 10 minutes before checking mails");
			Thread.currentThread().sleep(10 * 60 * 1000);
			sheet();
			return "done";
		} catch (Exception e) {
			LOG.error("Failed to make sheet", e);
			return "failed";
		}
	}
}