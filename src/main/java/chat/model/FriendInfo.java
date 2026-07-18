package chat.model;

/**
 * 好友信息（联表 reader_info 查询）
 */
public class FriendInfo {
    private int friendId;
    private String friendName;
    private int groupId;
    private String addTime;

    public FriendInfo(int friendId, String friendName, int groupId, String addTime) {
        this.friendId = friendId;
        this.friendName = friendName;
        this.groupId = groupId;
        this.addTime = addTime;
    }

    public int getFriendId() { return friendId; }
    public void setFriendId(int friendId) { this.friendId = friendId; }

    public String getFriendName() { return friendName; }
    public void setFriendName(String friendName) { this.friendName = friendName; }

    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }

    public String getAddTime() { return addTime; }
    public void setAddTime(String addTime) { this.addTime = addTime; }
}
