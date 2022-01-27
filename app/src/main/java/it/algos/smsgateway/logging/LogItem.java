package it.algos.smsgateway.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDateTime;

public class LogItem {

    private LocalDateTime time;
    private String lvl;
    private String msg;
    private Exception ex;

    public LogItem(LocalDateTime time, String lvl, String msg, Exception ex) {
        this.time=time;
        this.lvl=lvl;
        this.msg=msg;
        this.ex=ex;
    }

    public String getString(){

        StringBuffer sb=new StringBuffer();

        sb.append("["+time.toString()+"] "+lvl+": ");

        if(msg!=null){
            sb.append(msg);
        }

        if(ex!=null){
            Writer writer = new StringWriter();
            ex.printStackTrace(new PrintWriter(writer));
            if(msg!=null){
                sb.append(" ");
            }
            sb.append(writer.toString());
        }

        return sb.toString();
    }


    public LocalDateTime getTime() {
        return time;
    }

    public String getLvl() {
        return lvl;
    }

    public String getMsg() {
        return msg;
    }

    public Exception getEx() {
        return ex;
    }
}
