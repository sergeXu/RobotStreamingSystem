package cn.nodemedia.mediaclient.models;

public class Msg {

	public static final int TYPE_RECEIVED = 0;

	public static final int TYPE_SENT = 1;

	private String content;
	private long id;
	
	private String personName;

	private int type;
	
	public Msg(String personName,String content, int type,long id) {
		this.content = content;
		this.type = type;
		this.personName=personName;
		this.id=id;
	}
	public Msg(String personName,String content, int type) {
		this.content = content;
		this.type = type;
		this.personName=personName;
	}
	public Msg(String content, int type) {
		this("NULL", content, type);
	}

	public String getContent() {
		return content;
	}
	public String getPersonName() {
		return personName;
	}
	public long getId()
	{
		return this.id;
	}
	public int getType() {
		return type;
	}

}
