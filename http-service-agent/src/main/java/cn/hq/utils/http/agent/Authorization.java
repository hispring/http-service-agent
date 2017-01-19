package cn.hq.utils.http.agent;


/***
 * rest 身份认证配置
 * 
 * @author haiq
 *
 */
public class Authorization {

	/**@see cn.hq.utils.http.agent.AuthorizationAlgs*/
	private String alg;				//算法类型
	
	private String senderName;		//开发者注册的用户名
	
	private String secretKey;		//秘钥
	
	public Authorization(){
		
	}
	
	public Authorization(String senderName, String secretKey){
		this(AuthorizationAlgs.DEFAULT, senderName, secretKey);
	}
	
	public Authorization(String alg, String senderName, String secretKey){
		this.alg = alg;
		this.senderName = senderName;
		this.secretKey = secretKey;
	}

	public String getAlg() {
		return alg;
	}

	public void setAlg(String alg) {
		this.alg = alg;
	}

	public String getSenderName() {
		return senderName;
	}

	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}
	
	
}
