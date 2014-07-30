package btClient;

import java.io.DataOutputStream;
import java.io.IOException;

public class HandleSendMesssages {
	public static synchronized void handleSendMessages(sendMessages message){
		try {
			message.sendMessage();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
