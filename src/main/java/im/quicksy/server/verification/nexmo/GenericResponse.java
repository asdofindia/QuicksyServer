package im.quicksy.server.verification.nexmo;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GenericResponse {

    @SerializedName("message_count")
    private int messageCount;

    private List<Message> messages;

    public int getMessageCount() {
        return messageCount;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public static class Message {
        private String to;
        private String status;
        @SerializedName("error-text")
        private String errorText;

        public String getTo() {
            return to;
        }

        public String getStatus() {
            return status;
        }

        public String getErrorText() {
            return errorText;
        }
    }
}
