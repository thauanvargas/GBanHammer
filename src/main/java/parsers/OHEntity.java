package parsers;


import gearth.extensions.parsers.HEntityType;
import gearth.protocol.HPacket;

public class OHEntity {
    private int id;
    private int index;
    private String name;
    private String figureId;
    private String gender;
    private String motto;
    private int x;
    private int y;
    private String z;
    private String poolFigure;
    private String badgeCode;
    private HEntityType entityType;
    public OHEntity(HPacket packet) {
        this.index = packet.readInteger();
        this.name = packet.readString();
        this.figureId = packet.readString();
        this.gender = packet.readString();
        this.motto = packet.readString();
        this.x = packet.readInteger();
        this.y = packet.readInteger();
        this.z = packet.readString();
        this.poolFigure = packet.readString();
        this.badgeCode = packet.readString();
        int entityTypeId = packet.readInteger();
        this.entityType = HEntityType.valueOf(entityTypeId);
    }

    public static OHEntity[] parse(HPacket packet) {
        OHEntity[] entities = new OHEntity[packet.readInteger()];

        for(int i = 0; i < entities.length; ++i) {
            entities[i] = new OHEntity(packet);
        }

        return entities;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getMotto() {
        return motto;
    }

    public void setMotto(String motto) {
        this.motto = motto;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String getZ() {
        return z;
    }

    public void setZ(String z) {
        this.z = z;
    }

    public String getPoolFigure() {
        return poolFigure;
    }

    public void setPoolFigure(String poolFigure) {
        this.poolFigure = poolFigure;
    }

    public String getBadgeCode() {
        return badgeCode;
    }

    public void setBadgeCode(String badgeCode) {
        this.badgeCode = badgeCode;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFigureId() {
        return figureId;
    }

    public void setFigureId(String figureId) {
        this.figureId = figureId;
    }

    public HEntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(HEntityType entityType) {
        this.entityType = entityType;
    }
}
