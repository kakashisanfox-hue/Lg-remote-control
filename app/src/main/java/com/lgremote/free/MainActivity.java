package com.lgremote.free;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;
import javax.net.ssl.*;

public class MainActivity extends Activity {
  LinearLayout root, panel; EditText ip, text; TextView status; WebSocket ws, pointer;
  int seq=1; String pointerUrl="", host="";
  final int bg=0xff10131c, surface=0xff242a38, accent=0xff5b8cff;
  android.content.SharedPreferences prefs;
  interface Tap { void run(); }
  @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("remote",0);build();}
  GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(22);return d;}
  TextView button(String name, Tap action){TextView v=new TextView(this);v.setText(name);v.setTextColor(Color.WHITE);v.setTextSize(17);v.setGravity(17);v.setTypeface(null,Typeface.BOLD);v.setBackground(shape(surface));v.setPadding(9,17,9,17);v.setOnClickListener(w->{v.animate().alpha(.55f).setDuration(80).withEndAction(()->v.animate().alpha(1).setDuration(100).start()).start();action.run();});return v;}
  void add(LinearLayout l,String name,Tap action){TextView v=button(name,action);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(3,4,3,4);l.addView(v,p);}
  LinearLayout row(){LinearLayout r=new LinearLayout(this);r.setOrientation(0);panel.addView(r,new LinearLayout.LayoutParams(-1,-2));return r;}
  void build(){
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(18,20,18,20);root.setBackgroundColor(bg);scroll.addView(root);setContentView(scroll);
    TextView title=new TextView(this);title.setText("LG REMOTE");title.setTextColor(Color.WHITE);title.setTextSize(28);title.setTypeface(null,Typeface.BOLD);root.addView(title);
    status=new TextView(this);status.setText("Desconectado");status.setTextColor(0xffaab6cc);status.setPadding(0,8,0,10);root.addView(status);
    LinearLayout connect=new LinearLayout(this);connect.setOrientation(0);root.addView(connect);
    ip=new EditText(this);ip.setSingleLine(true);ip.setTextColor(-1);ip.setHintTextColor(0xff999999);ip.setHint("IP da TV (ex.: 192.168.1.10)");ip.setInputType(3);ip.setText(prefs.getString("ip",""));connect.addView(ip,new LinearLayout.LayoutParams(0,-2,1));
    TextView c=button("Conectar",()->connect());connect.addView(c);
    panel=new LinearLayout(this);panel.setOrientation(1);root.addView(panel);
    LinearLayout r=row();add(r,"⏻",()->request("ssap://system/turnOff",null));add(r,"HOME",()->key("HOME"));add(r,"VOLTAR",()->key("BACK"));
    r=row();add(r,"VOL +",()->request("ssap://audio/volumeUp",null));add(r,"MUDO",()->request("ssap://audio/setMute", "{\"mute\":true}"));add(r,"CH +",()->request("ssap://tv/channelUp",null));
    r=row();add(r,"VOL −",()->request("ssap://audio/volumeDown",null));add(r,"SOM",()->request("ssap://audio/setMute","{\"mute\":false}"));add(r,"CH −",()->request("ssap://tv/channelDown",null));
    r=row();add(r,"",()->{});add(r,"▲",()->key("UP"));add(r,"",()->{});
    r=row();add(r,"◀",()->key("LEFT"));add(r,"OK",()->key("ENTER"));add(r,"▶",()->key("RIGHT"));
    r=row();add(r,"",()->{});add(r,"▼",()->key("DOWN"));add(r,"",()->{});
    r=row();add(r,"NETFLIX",()->launch("netflix"));add(r,"PRIME",()->launch("amazon"));add(r,"YOUTUBE",()->launch("youtube.leanback.v4"));
    text=new EditText(this);text.setSingleLine(true);text.setTextColor(-1);text.setHintTextColor(0xff999999);text.setHint("Digite para enviar à TV");panel.addView(text);
    r=row();add(r,"ENVIAR TEXTO",()->{try{JSONObject p=new JSONObject();p.put("text",text.getText().toString());p.put("replace",0);request("ssap://com.webos.service.ime/insertText",p.toString());}catch(Exception e){}});add(r,"APAGAR",()->request("ssap://com.webos.service.ime/deleteCharacters","{\"count\":1}"));
  }
  void message(String s){runOnUiThread(()->status.setText(s));}
  void connect(){
    host=ip.getText().toString().trim();if(!host.matches("[0-9.]+")){message("Informe o IP da TV");return;}
    prefs.edit().putString("ip",host).apply();if(ws!=null)ws.close();
    message("Conectando...");ws=new WebSocket("ws://"+host+":3000",this::received,()->{message("Conectado: aceite o pareamento na TV");register();},e->message("Falha de conexão: "+e));
    ws.open();
  }
  void register(){try{
    JSONObject reg=new JSONObject();reg.put("type","register");reg.put("id","reg");
    JSONObject payload=new JSONObject();payload.put("forcePairing",false);payload.put("pairingType","PROMPT");
    String key=prefs.getString("key","");if(!key.isEmpty())payload.put("client-key",key);
    JSONObject manifest=new JSONObject();manifest.put("manifestVersion",1);
    manifest.put("appVersion","1.0");manifest.put("signed",new JSONObject().put("created","20261023").put("appId","com.lgremote.free").put("vendorId","com.lgremote.free").put("localizedAppNames",new JSONObject().put("","LG Remote")).put("localizedVendorNames",new JSONObject().put("","LG Remote")).put("permissions",new JSONArray("[\"LAUNCH\",\"LAUNCH_WEBAPP\",\"APP_TO_APP\",\"CONTROL_AUDIO\",\"CONTROL_DISPLAY\",\"CONTROL_INPUT_JOYSTICK\",\"CONTROL_INPUT_MEDIA_RECORDING\",\"CONTROL_INPUT_TV\",\"READ_INSTALLED_APPS\",\"READ_INPUT_DEVICE_LIST\",\"READ_NETWORK_STATE\",\"READ_TV_CHANNEL_LIST\",\"WRITE_NOTIFICATION_TOAST\"]")));
    manifest.put("permissions",new JSONArray("[\"LAUNCH\",\"CONTROL_AUDIO\",\"CONTROL_INPUT_JOYSTICK\",\"CONTROL_INPUT_TV\",\"READ_INSTALLED_APPS\"]"));
    payload.put("manifest",manifest);reg.put("payload",payload);ws.send(reg.toString());
  }catch(Exception e){message(e.toString());}}
  void received(String data){try{JSONObject j=new JSONObject(data);String type=j.optString("type");JSONObject p=j.optJSONObject("payload");
    if("registered".equals(type)){if(p!=null){String k=p.optString("client-key");if(!k.isEmpty())prefs.edit().putString("key",k).apply();}message("TV conectada ✓");request("ssap://com.webos.service.networkinput/getPointerInputSocket",null);}
    if("response".equals(type)&&p!=null&&p.has("socketPath")){pointerUrl=p.getString("socketPath");pointer=new WebSocket(pointerUrl,d->{},()->message("TV conectada ✓"),e->message("Controle direcional indisponível"));pointer.open();}
    if("error".equals(type))message("TV: "+j.optString("error"));
  }catch(Exception ignored){}}
  void request(String uri,String payload){if(ws==null||!ws.ready()){message("Conecte a TV primeiro");return;}try{JSONObject j=new JSONObject();j.put("id","req"+(seq++));j.put("type","request");j.put("uri",uri);if(payload!=null)j.put("payload",new JSONObject(payload));ws.send(j.toString());}catch(Exception e){message(e.toString());}}
  void key(String k){if(pointer==null||!pointer.ready()){message("Aguarde a conexão do controle");return;}pointer.send("type:button\nname:"+k+"\n\n");}
  void launch(String app){request("ssap://system.launcher/launch","{\"id\":\""+app+"\"}");}
  @Override protected void onDestroy(){super.onDestroy();if(ws!=null)ws.close();if(pointer!=null)pointer.close();}
  interface Receive {void accept(String s);}
  interface Failure {void accept(String s);}
  static class WebSocket {
    String url;Receive receive;Runnable connected;Failure failure;volatile boolean active=false;Socket socket;OutputStream out;
    WebSocket(String u,Receive r,Runnable c,Failure f){url=u;receive=r;connected=c;failure=f;}
    boolean ready(){return active;}
    void open(){new Thread(()->{try{
      URI uri=new URI(url);String h=uri.getHost();int port=uri.getPort();if(port<0)port=uri.getScheme().equals("wss")?443:80;
      socket=uri.getScheme().equals("wss")?SSLSocketFactory.getDefault().createSocket(h,port):new Socket(h,port);
      socket.setSoTimeout(15000);InputStream in=socket.getInputStream();out=socket.getOutputStream();
      byte[] nonce=new byte[16];new java.security.SecureRandom().nextBytes(nonce);String key=android.util.Base64.encodeToString(nonce,android.util.Base64.NO_WRAP);
      String path=uri.getRawPath();if(path==null||path.isEmpty())path="/";if(uri.getRawQuery()!=null)path+="?"+uri.getRawQuery();
      String head="GET "+path+" HTTP/1.1\r\nHost: "+h+":"+port+"\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: "+key+"\r\nSec-WebSocket-Version: 13\r\n\r\n";
      out.write(head.getBytes("UTF-8"));out.flush();ByteArrayOutputStream header=new ByteArrayOutputStream();int b;while((b=in.read())!=-1){header.write(b);byte[] a=header.toByteArray();int n=a.length;if(n>=4&&a[n-4]==13&&a[n-3]==10&&a[n-2]==13&&a[n-1]==10)break;if(n>8192)throw new IOException("Header too large");}
      String response=header.toString("UTF-8");if(!response.startsWith("HTTP/1.1 101")&&!response.startsWith("HTTP/1.0 101"))throw new IOException("Handshake rejeitado");
      String expected=android.util.Base64.encodeToString(java.security.MessageDigest.getInstance("SHA-1").digest((key+"258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8")),android.util.Base64.NO_WRAP);
      if(!response.toLowerCase(java.util.Locale.ROOT).contains("sec-websocket-accept: "+expected.toLowerCase(java.util.Locale.ROOT)))throw new IOException("Handshake inválido");
      active=true;connected.run();while(active){int first=in.read();if(first<0)break;int second=in.read();if(second<0)break;int op=first&15;long len=second&127;if(len==126)len=((long)read(in)<<8)|read(in);else if(len==127){len=0;for(int i=0;i<8;i++)len=(len<<8)|read(in);}if(len>1048576)throw new IOException("Frame grande");byte[] mask=null;if((second&128)!=0){mask=new byte[4];readFully(in,mask);}byte[] data=new byte[(int)len];readFully(in,data);if(mask!=null)for(int i=0;i<data.length;i++)data[i]^=mask[i%4];if(op==8)break;if(op==9){frame(10,data);continue;}if(op==1)receive.accept(new String(data,"UTF-8"));}
    }catch(Exception e){failure.accept(e.getMessage()==null?e.toString():e.getMessage());}finally{active=false;close();}}).start();}
    static int read(InputStream in)throws IOException{int x=in.read();if(x<0)throw new EOFException();return x;}
    static void readFully(InputStream in,byte[] data)throws IOException{int n=0;while(n<data.length){int x=in.read(data,n,data.length-n);if(x<0)throw new EOFException();n+=x;}}
    synchronized void frame(int opcode,byte[] bytes)throws IOException{if(!active||out==null)return;out.write(0x80|opcode);int n=bytes.length;if(n<126)out.write(0x80|n);else if(n<=65535){out.write(0x80|126);out.write(n>>8);out.write(n);}else{out.write(0x80|127);for(int i=7;i>=0;i--)out.write((n>>(8*i))&255);}byte[] mask=new byte[4];new java.security.SecureRandom().nextBytes(mask);out.write(mask);for(int i=0;i<n;i++)out.write(bytes[i]^mask[i%4]);out.flush();}
    void send(String s){try{frame(1,s.getBytes("UTF-8"));}catch(Exception e){failure.accept(e.toString());}}
    void close(){active=false;try{if(socket!=null)socket.close();}catch(Exception ignored){}}
  }
}
