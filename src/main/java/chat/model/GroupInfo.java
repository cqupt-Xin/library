package chat.model;

import java.util.List;

/**
 * 群组信息实体
 */
public class GroupInfo {
    private int groupId;          // 群ID
    private String groupName;     // 群名称
    private int ownerId;          // 群主ID
    private String ownerName;     // 群主名
    private String createTime;    // 创建时间
    private int memberCount;      // 成员数
    private String avatar;        // 群头像（默认空）
    private List<String> members; // 群成员用户名列表

    public GroupInfo() {}

    public GroupInfo(int groupId, String groupName, int ownerId, String ownerName, String createTime) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.createTime = createTime;
    }

    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }
}
