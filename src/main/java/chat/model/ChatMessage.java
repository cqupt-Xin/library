package chat.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 聊天消息实体
 * 支持私聊和群聊两种类型
 */
public class ChatMessage {
    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private long msgId;          // 消息唯一ID（自增）
    private int senderId;        // 发送者用户ID
    private String senderName;   // 发送者用户名
    private int targetId;        // 接收者ID（私聊:用户ID, 群聊:群ID）
    private String msgType;      // private / group / system
    private String content;      // 消息内容
    private String sendTime;     // 发送时间
    private boolean read;        // 是否已读（私聊用）
    private int readCount;       // 群聊已读人数

    public ChatMessage() {}

    public ChatMessage(int senderId, String senderName, int targetId, String msgType, String content) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.targetId = targetId;
        this.msgType = msgType;
        this.content = content;
        this.sendTime = LocalDateTime.now().format(TIME_FMT);
        this.read = false;
    }

    // Getters and Setters
    public long getMsgId() { return msgId; }
    public void setMsgId(long msgId) { this.msgId = msgId; }

    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public int getTargetId() { return targetId; }
    public void setTargetId(int targetId) { this.targetId = targetId; }

    public String getMsgType() { return msgType; }
    public void setMsgType(String msgType) { this.msgType = msgType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSendTime() { return sendTime; }
    public void setSendTime(String sendTime) { this.sendTime = sendTime; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public int getReadCount() { return readCount; }
    public void setReadCount(int readCount) { this.readCount = readCount; }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s", msgType, senderName, content);
    }
}
