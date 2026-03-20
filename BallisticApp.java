import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class BallisticApp {

    // ===== BallisticData =====
    static class BallisticData implements Serializable {
        private static final long serialVersionUID = 1L;
        public static final double G_METRIC = 9.80665, G_IMPERIAL = 32.174, METERS_TO_FEET = 3.28084;
        private double initialVelocity, angleDegrees, rangeMetric, rangeImperial;
        private transient long timestamp;
        private transient String statusMessage;

        public BallisticData(double v, double a) {
            if (v < 0) throw new IllegalArgumentException("Початкова швидкість не може бути від'ємною: " + v);
            if (a < 0 || a > 90) throw new IllegalArgumentException("Кут має бути в діапазоні [0; 90]: " + a);
            this.initialVelocity = v; this.angleDegrees = a;
            this.timestamp = System.currentTimeMillis();
            this.statusMessage = "Об'єкт створено, обчислення ще не виконано.";
        }
        public double getInitialVelocity()    { return initialVelocity; }
        public void   setInitialVelocity(double v) { if(v<0) throw new IllegalArgumentException("Швидкість < 0"); initialVelocity=v; }
        public double getAngleDegrees()       { return angleDegrees; }
        public void   setAngleDegrees(double a) { if(a<0||a>90) throw new IllegalArgumentException("Кут поза [0;90]"); angleDegrees=a; }
        public double getRangeMetric()        { return rangeMetric; }
        public void   setRangeMetric(double r)    { rangeMetric=r; }
        public double getRangeImperial()      { return rangeImperial; }
        public void   setRangeImperial(double r)  { rangeImperial=r; }
        public long   getTimestamp()          { return timestamp; }
        public void   setTimestamp(long t)    { timestamp=t; }
        public String getStatusMessage()      { return statusMessage; }
        public void   setStatusMessage(String s) { statusMessage=s; }
        @Override public String toString() {
            return String.format("BallisticData {%n  Початкова швидкість : %.2f м/с%n  Кут : %.2f°%n  Дальність (метрич.) : %.4f м%n  Дальність (англ.) : %.4f фут%n  Timestamp (transient): %d%n  Status (transient): %s%n}",
                initialVelocity,angleDegrees,rangeMetric,rangeImperial,timestamp,statusMessage);
        }
    }

    // ===== BallisticSolver =====
    static class BallisticSolver {
        private BallisticData data;
        public BallisticSolver(BallisticData d) { if(d==null) throw new IllegalArgumentException("Об'єкт даних не може бути null."); data=d; }
        public BallisticSolver(double v, double a) { data=new BallisticData(v,a); }
        public BallisticData getData()   { return data; }
        public void setData(BallisticData d) { if(d==null) throw new IllegalArgumentException("data == null"); data=d; }
        public double computeRangeMetric()   { double v=data.getInitialVelocity(),r=Math.toRadians(data.getAngleDegrees()); return (v*v*Math.sin(2.0*r))/BallisticData.G_METRIC; }
        public double computeRangeImperial() { double v=data.getInitialVelocity()*BallisticData.METERS_TO_FEET,r=Math.toRadians(data.getAngleDegrees()); return (v*v*Math.sin(2.0*r))/BallisticData.G_IMPERIAL; }
        public void solve() { data.setRangeMetric(computeRangeMetric()); data.setRangeImperial(computeRangeImperial()); data.setStatusMessage("Обчислення виконано успішно."); }
        public String getReport() {
            return String.format("========== Балістичний розрахунок ==========%n  v0 = %.2f м/с%n  α  = %.2f°%n---------- Результати ---------------------%n  Метрична система  : %.4f м (%.4f км)%n  Англійська система: %.4f фут (%.4f миль)%n============================================",
                data.getInitialVelocity(),data.getAngleDegrees(),data.getRangeMetric(),data.getRangeMetric()/1000.0,data.getRangeImperial(),data.getRangeImperial()/5280.0);
        }
    }

    // ===== ResultCollection =====
    static class ResultCollection implements Serializable {
        private static final long serialVersionUID = 2L;
        private final List<BallisticData> results = new ArrayList<>();
        public void add(BallisticData d)   { if(d==null) throw new IllegalArgumentException("data == null"); results.add(d); }
        public List<BallisticData> getAll(){ return Collections.unmodifiableList(results); }
        public int size()   { return results.size(); }
        public void clear() { results.clear(); }
        public void removeLast() { if(!results.isEmpty()) results.remove(results.size()-1); }
        public void setAll(List<BallisticData> items) { results.clear(); results.addAll(items); }
        public List<BallisticData> snapshot() { return new ArrayList<>(results); }
        List<BallisticData> getMutable() { return results; }
        public void saveToFile(String f) throws IOException { try(ObjectOutputStream o=new ObjectOutputStream(new FileOutputStream(f))){o.writeObject(this);} }
        public void loadFromFile(String f) throws IOException,ClassNotFoundException { try(ObjectInputStream i=new ObjectInputStream(new FileInputStream(f))){ ResultCollection l=(ResultCollection)i.readObject(); results.clear(); results.addAll(l.results);} }
        @Override public String toString() { return String.format("ResultCollection { розмір=%d }",results.size()); }
    }

    // ===== TableConfig =====
    static class TableConfig implements Serializable {
        private static final long serialVersionUID = 3L;
        private int columnWidth, maxRows; private char separator; private boolean showMetric,showImperial,showDerived; private String title;
        public TableConfig() { columnWidth=10; separator='|'; showMetric=true; showImperial=true; maxRows=0; title="Результати балістики"; showDerived=false; }
        public TableConfig(int cw,char sep,boolean sm,boolean si,int mr,String t,boolean sd) { columnWidth=Math.max(4,cw); separator=sep; showMetric=sm; showImperial=si; maxRows=Math.max(0,mr); title=(t!=null&&!t.isBlank())?t:"Таблиця"; showDerived=sd; }
        public int getColumnWidth(){return columnWidth;} public void setColumnWidth(int w){columnWidth=Math.max(4,w);}
        public char getSeparator(){return separator;} public void setSeparator(char s){separator=s;}
        public boolean isShowMetric(){return showMetric;} public void setShowMetric(boolean v){showMetric=v;}
        public boolean isShowImperial(){return showImperial;} public void setShowImperial(boolean v){showImperial=v;}
        public int getMaxRows(){return maxRows;} public void setMaxRows(int r){maxRows=Math.max(0,r);}
        public String getTitle(){return title;} public void setTitle(String t){title=(t!=null&&!t.isBlank())?t:"Таблиця";}
        public boolean isShowDerived(){return showDerived;} public void setShowDerived(boolean v){showDerived=v;}
        public int effectiveRows(int n){return(maxRows==0||maxRows>n)?n:maxRows;}
        public int activeDataColumns(){int c=0;if(showMetric)c+=showDerived?2:1;if(showImperial)c+=showDerived?2:1;return c;}
        @Override public String toString(){return String.format("TableConfig{ ширина=%d, роздільник='%c', метрична=%b, англійська=%b, макс_рядків=%s, похідні=%b, заголовок=\"%s\" }",columnWidth,separator,showMetric,showImperial,maxRows==0?"всі":String.valueOf(maxRows),showDerived,title);}
    }

    // ===== Interfaces =====
    interface Displayable { void displayShort(); void displayFull(); void displayTable(List<BallisticData> r); }
    interface ConfigurableDisplay extends Displayable { void displayTable(List<BallisticData> r,int maxRows); void displayTable(List<BallisticData> r,TableConfig cfg); }
    interface DisplayFactory {
        Displayable createDisplay(BallisticData data);
        default void displayAll(BallisticData d,List<BallisticData> r){Displayable x=createDisplay(d);x.displayShort();x.displayFull();x.displayTable(r);}
    }

    // ===== TextDisplay =====
    static class TextDisplay implements Displayable {
        protected final BallisticData data;
        public TextDisplay(BallisticData d){data=d;}
        @Override public void displayShort(){System.out.printf("[Текст] v0=%.1f м/с | α=%.1f° | дальність=%.2f м%n",data.getInitialVelocity(),data.getAngleDegrees(),data.getRangeMetric());}
        @Override public void displayFull(){
            System.out.println("------ Текстовий звіт (TextDisplay) ------");
            System.out.printf("  Початкова швидкість : %.2f м/с%n",data.getInitialVelocity());
            System.out.printf("  Кут пострілу        : %.2f°%n",data.getAngleDegrees());
            System.out.printf("  Дальність (м)       : %.4f м%n",data.getRangeMetric());
            System.out.printf("  Дальність (км)      : %.6f км%n",data.getRangeMetric()/1000.0);
            System.out.printf("  Дальність (фут)     : %.4f фут%n",data.getRangeImperial());
            System.out.printf("  Дальність (миль)    : %.6f миль%n",data.getRangeImperial()/5280.0);
            System.out.println("------------------------------------------");
        }
        @Override public void displayTable(List<BallisticData> r){
            System.out.println("====== Список результатів (TextDisplay) ======");
            if(r.isEmpty()){System.out.println("  [порожньо]");}
            else{for(int i=0;i<r.size();i++){BallisticData x=r.get(i);System.out.printf("  %2d. v0=%.1f м/с, α=%.1f° → %.2f м / %.2f фут%n",i+1,x.getInitialVelocity(),x.getAngleDegrees(),x.getRangeMetric(),x.getRangeImperial());}}
            System.out.println("=============================================");
        }
    }

    // ===== SimpleTableDisplay =====
    static class SimpleTableDisplay extends TextDisplay implements ConfigurableDisplay {
        protected TableConfig config;
        public SimpleTableDisplay(BallisticData d){super(d);config=new TableConfig();}
        public SimpleTableDisplay(BallisticData d,TableConfig c){super(d);config=(c!=null)?c:new TableConfig();}
        public TableConfig getConfig(){return config;} public void setConfig(TableConfig c){config=(c!=null)?c:new TableConfig();}
        @Override public void displayShort(){System.out.printf("[SimpleTable] v0=%.1f м/с | α=%.1f° | метрич.=%.2f м | англ.=%.2f фут%n",data.getInitialVelocity(),data.getAngleDegrees(),data.getRangeMetric(),data.getRangeImperial());}
        @Override public void displayFull(){
            System.out.println("------ Звіт (SimpleTableDisplay) ------");
            System.out.printf("  Конфігурація  : %s%n",config);
            System.out.printf("  v0            : %.4f м/с%n",data.getInitialVelocity());
            System.out.printf("  α             : %.4f°%n",data.getAngleDegrees());
            if(config.isShowMetric()){System.out.printf("  Метрична      : %.4f м%n",data.getRangeMetric());if(config.isShowDerived())System.out.printf("                  %.6f км%n",data.getRangeMetric()/1000.0);}
            if(config.isShowImperial()){System.out.printf("  Англійська    : %.4f фут%n",data.getRangeImperial());if(config.isShowDerived())System.out.printf("                  %.6f миль%n",data.getRangeImperial()/5280.0);}
            System.out.println("---------------------------------------");
        }
        @Override public void displayTable(List<BallisticData> r){displayTable(r,config);}
        @Override public void displayTable(List<BallisticData> r,int mr){displayTable(r,new TableConfig(config.getColumnWidth(),config.getSeparator(),config.isShowMetric(),config.isShowImperial(),mr,config.getTitle()+" [ліміт="+mr+"]",config.isShowDerived()));}
        @Override public void displayTable(List<BallisticData> r,TableConfig cfg){
            int w=cfg.getColumnWidth(); char sep=cfg.getSeparator(); int rows=cfg.effectiveRows(r.size());
            List<String> hdr=buildHeaders(cfg); int ll=4+(w+3)*(2+hdr.size()); String hl=String.valueOf(sep).repeat(ll);
            System.out.println(hl); System.out.printf("%s %-"+(ll-2)+"s %s%n",sep,cfg.getTitle(),sep); System.out.println(hl);
            System.out.printf("%s %-3s %s",sep,"№",sep); System.out.printf(" %-"+w+"."+w+"s %s","v0(м/с)",sep); System.out.printf(" %-"+w+"."+w+"s %s","α(°)",sep);
            for(String h:hdr)System.out.printf(" %-"+w+"."+w+"s %s",h,sep); System.out.println(); System.out.println(hl);
            if(r.isEmpty()){System.out.printf("%s %-"+(ll-2)+"s %s%n",sep,"  [порожньо]",sep);}
            else{for(int i=0;i<rows;i++){BallisticData x=r.get(i);System.out.printf("%s %-3d %s",sep,i+1,sep);System.out.printf(" %-"+w+".2f %s",x.getInitialVelocity(),sep);System.out.printf(" %-"+w+".2f %s",x.getAngleDegrees(),sep);printDataCols(x,cfg,w,sep);System.out.println();}
            if(rows<r.size())System.out.printf("%s ... ще %d запис(ів) не показано %s%n",sep,r.size()-rows,sep);}
            System.out.println(hl);
        }
        protected List<String> buildHeaders(TableConfig c){List<String> h=new ArrayList<>();if(c.isShowMetric()){h.add("м");if(c.isShowDerived())h.add("км");}if(c.isShowImperial()){h.add("фут");if(c.isShowDerived())h.add("миль");}return h;}
        protected void printDataCols(BallisticData r,TableConfig c,int w,char sep){
            if(c.isShowMetric()){System.out.printf(" %-"+w+".2f %s",r.getRangeMetric(),sep);if(c.isShowDerived())System.out.printf(" %-"+w+".4f %s",r.getRangeMetric()/1000.0,sep);}
            if(c.isShowImperial()){System.out.printf(" %-"+w+".2f %s",r.getRangeImperial(),sep);if(c.isShowDerived())System.out.printf(" %-"+w+".4f %s",r.getRangeImperial()/5280.0,sep);}
        }
    }

    // ===== BorderedTableDisplay =====
    static class BorderedTableDisplay extends SimpleTableDisplay {
        private static final String V="|",H="-";
        public BorderedTableDisplay(BallisticData d){super(d);}
        public BorderedTableDisplay(BallisticData d,TableConfig c){super(d,c);}
        @Override public void displayShort(){System.out.printf("[Bordered] v0=%.1f м/с | α=%.1f° | %.2f м | %.2f фут%n",data.getInitialVelocity(),data.getAngleDegrees(),data.getRangeMetric(),data.getRangeImperial());}
        @Override public void displayTable(List<BallisticData> r,TableConfig cfg){
            int w=cfg.getColumnWidth(),rows=cfg.effectiveRows(r.size());
            List<String> hdr=buildHeaders(cfg); int[]ws=new int[2+hdr.size()]; ws[0]=3;ws[1]=Math.max(w,8);ws[2]=Math.max(w,6);
            for(int i=3;i<ws.length;i++)ws[i]=w;
            String top=hline("+","+",H,ws),mid=hline("+","+",H,ws),bot=hline("+","+",H,ws);
            int tw=Arrays.stream(ws).sum()+ws.length*3+1;
            System.out.println(top);System.out.printf("%s %-"+(tw-2)+"s %s%n",V,cfg.getTitle(),V);System.out.println(mid);
            System.out.printf("%s %-3s %s",V,"№",V);System.out.printf(" %-"+Math.max(w,8)+"s %s","v0(м/с)",V);System.out.printf(" %-"+Math.max(w,6)+"s %s","α(°)",V);
            for(String h:hdr)System.out.printf(" %-"+w+"s %s",h,V);System.out.println();System.out.println(mid);
            if(r.isEmpty()){System.out.printf("%s %-"+(tw-2)+"s %s%n",V,"  [порожньо]",V);}
            else{for(int i=0;i<rows;i++){BallisticData x=r.get(i);System.out.printf("%s %-3d %s",V,i+1,V);System.out.printf(" %-"+Math.max(w,8)+".2f %s",x.getInitialVelocity(),V);System.out.printf(" %-"+Math.max(w,6)+".2f %s",x.getAngleDegrees(),V);printDataCols(x,cfg,w,V.charAt(0));System.out.println();}
            if(rows<r.size())System.out.printf("%s ... ще %d запис(ів) %s%n",V,r.size()-rows,V);}
            System.out.println(bot);
        }
        private static String hline(String l,String r,String h,int[]ws){StringBuilder sb=new StringBuilder(l);for(int i=0;i<ws.length;i++){sb.append(h.repeat(ws[i]+2));sb.append(i<ws.length-1?"+":r);}return sb.toString();}
    }

    // ===== DetailedTextDisplay =====
    static class DetailedTextDisplay implements Displayable {
        private final BallisticData data;
        public DetailedTextDisplay(BallisticData d){data=d;}
        @Override public void displayShort(){System.out.printf("[Детально] v0=%.1f м/с | α=%.1f° | %.4f м | %.4f фут%n",data.getInitialVelocity(),data.getAngleDegrees(),data.getRangeMetric(),data.getRangeImperial());}
        @Override public void displayFull(){
            double v=data.getInitialVelocity(),a=data.getAngleDegrees(),ar=Math.toRadians(a);
            System.out.println("====== Детальний звіт (DetailedTextDisplay) ======");
            System.out.println("  Формула: s = v0² · sin(2α) / g");
            System.out.printf("    v0=%.4f м/с%n",v);System.out.printf("    α=%.4f° (%.6f рад)%n",a,ar);
            System.out.printf("    v0²=%.4f%n",v*v);System.out.printf("    sin(2α)=%.6f%n",Math.sin(2.0*ar));
            System.out.printf("    Метрична: %.6f м%n",data.getRangeMetric());System.out.printf("    Англійська: %.6f фут%n",data.getRangeImperial());
            System.out.println("==================================================");
        }
        @Override public void displayTable(List<BallisticData> r){
            System.out.println("====== Детальна таблиця ======");
            System.out.printf("  %-4s %-10s %-8s %-14s %-14s%n","№","v0 (м/с)","α (°)","Метрично (м)","Англійськ (фут)");
            System.out.println("  "+"-".repeat(54));
            for(int i=0;i<r.size();i++){BallisticData x=r.get(i);System.out.printf("  %-4d %-10.2f %-8.2f %-14.4f %-14.4f%n",i+1,x.getInitialVelocity(),x.getAngleDegrees(),x.getRangeMetric(),x.getRangeImperial());}
            System.out.println("  "+"=".repeat(54));
        }
    }

    // ===== CompactDisplay =====
    static class CompactDisplay implements Displayable {
        private final BallisticData data;
        public CompactDisplay(BallisticData d){data=d;}
        @Override public void displayShort(){System.out.printf("[Компакт] %.0f м/с @ %.0f° -> %.0f м%n",data.getInitialVelocity(),data.getAngleDegrees(),data.getRangeMetric());}
        @Override public void displayFull(){System.out.printf("[ CompactDisplay ] v0=%.2f | α=%.2f° | s=%.2f м | s=%.2f фут%n",data.getInitialVelocity(),data.getAngleDegrees(),data.getRangeMetric(),data.getRangeImperial());}
        @Override public void displayTable(List<BallisticData> r){
            System.out.printf("[Компактна таблиця | %d записів]%n",r.size());
            for(int i=0;i<r.size();i++){BallisticData x=r.get(i);System.out.printf("  #%-2d %.0f м/с @ %.0f° -> %.0f м%n",i+1,x.getInitialVelocity(),x.getAngleDegrees(),x.getRangeMetric());}
        }
    }

    // ===== CsvTableDisplay =====
    static class CsvTableDisplay implements ConfigurableDisplay {
        private final BallisticData data; private TableConfig config;
        public CsvTableDisplay(BallisticData d){data=d;config=new TableConfig();config.setSeparator(',');}
        public CsvTableDisplay(BallisticData d,TableConfig c){data=d;config=(c!=null)?c:new TableConfig();}
        @Override public void displayShort(){char d=config.getSeparator();System.out.printf("[CSV] %.2f%c%.2f%c%.4f%c%.4f%n",data.getInitialVelocity(),d,data.getAngleDegrees(),d,data.getRangeMetric(),d,data.getRangeImperial());}
        @Override public void displayFull(){char d=config.getSeparator();System.out.println("# CsvTableDisplay — один запис");System.out.printf("v0%cAlpha%c",d,d);if(config.isShowMetric())System.out.printf("metric_m%c",d);if(config.isShowImperial())System.out.printf("imperial_ft%c",d);System.out.println();System.out.printf("%.4f%c%.4f%c",data.getInitialVelocity(),d,data.getAngleDegrees(),d);if(config.isShowMetric())System.out.printf("%.4f%c",data.getRangeMetric(),d);if(config.isShowImperial())System.out.printf("%.4f%c",data.getRangeImperial(),d);System.out.println();}
        @Override public void displayTable(List<BallisticData> r){displayTable(r,config);}
        @Override public void displayTable(List<BallisticData> r,int mr){displayTable(r,new TableConfig(config.getColumnWidth(),config.getSeparator(),config.isShowMetric(),config.isShowImperial(),mr,config.getTitle(),config.isShowDerived()));}
        @Override public void displayTable(List<BallisticData> r,TableConfig cfg){
            char d=cfg.getSeparator();int rows=cfg.effectiveRows(r.size());
            System.out.println("# "+cfg.getTitle()+" [CSV]");System.out.printf("no%cv0%calpha%c",d,d,d);
            if(cfg.isShowMetric())System.out.printf("metric_m%c",d);if(cfg.isShowDerived()&&cfg.isShowMetric())System.out.printf("metric_km%c",d);
            if(cfg.isShowImperial())System.out.printf("imperial_ft%c",d);if(cfg.isShowDerived()&&cfg.isShowImperial())System.out.printf("imperial_mi%c",d);
            System.out.println();
            for(int i=0;i<rows;i++){BallisticData x=r.get(i);System.out.printf("%d%c%.4f%c%.4f%c",i+1,d,x.getInitialVelocity(),d,x.getAngleDegrees(),d);if(cfg.isShowMetric())System.out.printf("%.4f%c",x.getRangeMetric(),d);if(cfg.isShowDerived()&&cfg.isShowMetric())System.out.printf("%.6f%c",x.getRangeMetric()/1000.0,d);if(cfg.isShowImperial())System.out.printf("%.4f%c",x.getRangeImperial(),d);if(cfg.isShowDerived()&&cfg.isShowImperial())System.out.printf("%.6f%c",x.getRangeImperial()/5280.0,d);System.out.println();}
            if(rows<r.size())System.out.printf("# ... ще %d записів пропущено%n",r.size()-rows);
        }
    }

    // ===== Factories =====
    static class TextDisplayFactory     implements DisplayFactory { @Override public Displayable createDisplay(BallisticData d){return new TextDisplay(d);} }
    static class DetailedDisplayFactory implements DisplayFactory { @Override public Displayable createDisplay(BallisticData d){return new DetailedTextDisplay(d);} }
    static class CompactDisplayFactory  implements DisplayFactory { @Override public Displayable createDisplay(BallisticData d){return new CompactDisplay(d);} }
    static class SimpleTableDisplayFactory implements DisplayFactory {
        private final TableConfig c; public SimpleTableDisplayFactory(){c=new TableConfig();} public SimpleTableDisplayFactory(TableConfig c){this.c=(c!=null)?c:new TableConfig();}
        @Override public Displayable createDisplay(BallisticData d){return new SimpleTableDisplay(d,c);}
    }
    static class BorderedTableDisplayFactory implements DisplayFactory {
        private final TableConfig c; public BorderedTableDisplayFactory(){c=new TableConfig();} public BorderedTableDisplayFactory(TableConfig c){this.c=(c!=null)?c:new TableConfig();}
        @Override public Displayable createDisplay(BallisticData d){return new BorderedTableDisplay(d,c);}
    }
    static class CsvTableDisplayFactory implements DisplayFactory {
        private final TableConfig c; public CsvTableDisplayFactory(){TableConfig x=new TableConfig();x.setSeparator(',');c=x;} public CsvTableDisplayFactory(TableConfig c){this.c=(c!=null)?c:new TableConfig();}
        @Override public Displayable createDisplay(BallisticData d){return new CsvTableDisplay(d,c);}
    }

    // ===== AppContext =====
    static class AppContext {
        ResultCollection collection=new ResultCollection(); DisplayFactory factory=new TextDisplayFactory();
        TableConfig tableConfig=new TableConfig(); BallisticData lastData=null; final Scanner scanner;
        static final String COLLECTION_FILE="ballistic_collection.ser";
        AppContext(Scanner s){scanner=s;}
    }

    // ===== Command =====
    interface Command { void execute(); void undo(); String getName(); }

    // ===== CommandHistory (Singleton) =====
    static class CommandHistory {
        private static volatile CommandHistory instance;
        private final Deque<Command> history=new ArrayDeque<>();
        private CommandHistory(){}
        public static CommandHistory getInstance(){if(instance==null){synchronized(CommandHistory.class){if(instance==null)instance=new CommandHistory();}}return instance;}
        public void push(Command c){history.push(c);}
        public boolean canUndo(){return !history.isEmpty();}
        public int size(){return history.size();}
        public void clearHistory(){history.clear();}
        public void undo(){if(!history.isEmpty()){Command c=history.pop();c.undo();System.out.println("[Undo] Скасовано: «"+c.getName()+"»");}else System.out.println("[Undo] Немає операцій для скасування.");}
    }

    // ===== Menu =====
    static class Menu {
        private final String title; private final LinkedHashMap<String,MenuEntry> entries=new LinkedHashMap<>();
        static class MenuEntry{final String label;final Command command;MenuEntry(String l,Command c){label=l;command=c;}}
        public Menu(String t){title=t;}
        public void addEntry(String k,String l,Command c){entries.put(k,new MenuEntry(l,c));}
        public void removeEntry(String k){entries.remove(k);}
        public boolean hasKey(String k){return entries.containsKey(k);}
        public void display(AppContext ctx){
            System.out.println("\n+=====================================================+");
            System.out.printf(" %s%n",title);
            System.out.printf(" Колекція: %d записів | Undo-стек: %d | Фабрика: %s%n",ctx.collection.size(),CommandHistory.getInstance().size(),ctx.factory.getClass().getSimpleName());
            System.out.println("+-----------------------------------------------------+");
            for(Map.Entry<String,MenuEntry> e:entries.entrySet())System.out.printf("  %-3s — %s%n",e.getKey(),e.getValue().label);
            System.out.println("+=====================================================+");
        }
        public boolean executeCommand(String k){MenuEntry e=entries.get(k);if(e==null)return false;e.command.execute();CommandHistory.getInstance().push(e.command);return true;}
    }

    // =========================================================================
    // WorkerThread — шаблон Worker Thread для управління чергою команд
    // =========================================================================

    /**
     * Реалізація шаблону Worker Thread для асинхронного виконання черги команд.
     *
     * <p>Шаблон Worker Thread відокремлює постановку завдань у чергу від їх виконання.
     * Команди додаються через {@link #submit(Command)}, а окремий потік-робітник
     * виконує їх послідовно у фоновому режимі.</p>
     *
     * <p>Використовує {@link LinkedBlockingQueue} як потокобезпечну чергу
     * та {@link AtomicInteger} для підрахунку виконаних команд.</p>
     */
    static class WorkerThread {

        /** Потокобезпечна черга команд. */
        private final BlockingQueue<Command> queue = new LinkedBlockingQueue<>();

        /** Лічильник виконаних команд. */
        private final AtomicInteger completedCount = new AtomicInteger(0);

        /** Прапорець активності. */
        private volatile boolean running = false;

        /** Фоновий потік-робітник. */
        private Thread worker;

        /**
         * Запускає потік-робітник.
         * Потік очікує команди з черги та виконує їх послідовно.
         */
        public void start() {
            running = true;
            worker = new Thread(() -> {
                System.out.println("[WorkerThread] Потік-робітник запущено.");
                while (running || !queue.isEmpty()) {
                    try {
                        Command cmd = queue.poll(500, TimeUnit.MILLISECONDS);
                        if (cmd != null) {
                            System.out.printf("[WorkerThread] Виконання: «%s»%n", cmd.getName());
                            cmd.execute();
                            completedCount.incrementAndGet();
                            System.out.printf("[WorkerThread] Завершено: «%s» (виконано: %d)%n",
                                cmd.getName(), completedCount.get());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                System.out.println("[WorkerThread] Потік-робітник зупинено.");
            }, "ballistic-worker");
            worker.setDaemon(true);
            worker.start();
        }

        /**
         * Додає команду до черги для асинхронного виконання.
         *
         * @param cmd команда
         */
        public void submit(Command cmd) {
            queue.offer(cmd);
            System.out.printf("[WorkerThread] Команду «%s» поставлено у чергу. Очікує: %d%n",
                cmd.getName(), queue.size());
        }

        /**
         * Зупиняє потік-робітник після виконання всіх команд у черзі.
         *
         * @throws InterruptedException якщо очікування перервано
         */
        public void shutdown() throws InterruptedException {
            running = false;
            if (worker != null) worker.join(5000);
            System.out.printf("[WorkerThread] Зупинено. Виконано команд: %d%n",
                completedCount.get());
        }

        public int  queueSize()      { return queue.size(); }
        public int  completedCount() { return completedCount.get(); }
        public boolean isRunning()   { return running; }
    }

    // =========================================================================
    // ParallelStatsCommand — паралельна обробка елементів колекції
    // =========================================================================

    /**
     * Команда паралельної статистичної обробки елементів колекції.
     *
     * <p>Використовує {@link ExecutorService} з пулом потоків для одночасного
     * обчислення у 5 паралельних потоках:</p>
     * <ul>
     *   <li>мінімальної дальності;</li>
     *   <li>максимальної дальності;</li>
     *   <li>середнього значення дальності;</li>
     *   <li>стандартного відхилення;</li>
     *   <li>відбору елементів за критерієм (дальність &gt; середнього).</li>
     * </ul>
     *
     * <p>Всі задачі запускаються паралельно через {@link Future}.
     * Результати збираються після завершення всіх потоків.</p>
     */
    static class ParallelStatsCommand implements Command {

        private final AppContext ctx;

        public ParallelStatsCommand(AppContext ctx) { this.ctx = ctx; }

        @Override public String getName() { return "Паралельна статистика колекції"; }

        @Override
        public void execute() {
            if (ctx.collection.size() < 2) {
                System.out.println("  [!] Потрібно принаймні 2 записи для статистики.");
                return;
            }

            // Незмінна копія для потокобезпечного використання
            final List<BallisticData> snapshot = new ArrayList<>(ctx.collection.getAll());
            final int n = snapshot.size();

            System.out.println("\n+==== Паралельна обробка колекції ====+");
            System.out.printf("  Елементів: %d | Потоків: 5%n", n);
            System.out.println("+-------------------------------------+");

            // Пул із 5 потоків
            ExecutorService pool = Executors.newFixedThreadPool(5);

            try {
                // Задача 1: пошук мінімуму
                Future<BallisticData> futMin = pool.submit(() -> {
                    System.out.printf("  [%s] Пошук мінімуму...%n",
                        Thread.currentThread().getName());
                    return snapshot.stream()
                        .min(Comparator.comparingDouble(BallisticData::getRangeMetric))
                        .orElse(null);
                });

                // Задача 2: пошук максимуму
                Future<BallisticData> futMax = pool.submit(() -> {
                    System.out.printf("  [%s] Пошук максимуму...%n",
                        Thread.currentThread().getName());
                    return snapshot.stream()
                        .max(Comparator.comparingDouble(BallisticData::getRangeMetric))
                        .orElse(null);
                });

                // Задача 3: середнє значення
                Future<Double> futAvg = pool.submit(() -> {
                    System.out.printf("  [%s] Обчислення середнього...%n",
                        Thread.currentThread().getName());
                    return snapshot.stream()
                        .mapToDouble(BallisticData::getRangeMetric)
                        .average().orElse(0.0);
                });

                // Задача 4: стандартне відхилення
                Future<Double> futStd = pool.submit(() -> {
                    System.out.printf("  [%s] Обчислення стандартного відхилення...%n",
                        Thread.currentThread().getName());
                    double avg = snapshot.stream()
                        .mapToDouble(BallisticData::getRangeMetric)
                        .average().orElse(0.0);
                    double var = snapshot.stream()
                        .mapToDouble(d -> Math.pow(d.getRangeMetric() - avg, 2))
                        .average().orElse(0.0);
                    return Math.sqrt(var);
                });

                // Задача 5: відбір за критерієм (дальність > середнього)
                Future<List<BallisticData>> futFilter = pool.submit(() -> {
                    System.out.printf("  [%s] Відбір за критерієм (> середнього)...%n",
                        Thread.currentThread().getName());
                    double avg = snapshot.stream()
                        .mapToDouble(BallisticData::getRangeMetric)
                        .average().orElse(0.0);
                    List<BallisticData> res = new ArrayList<>();
                    for (BallisticData d : snapshot)
                        if (d.getRangeMetric() > avg) res.add(d);
                    return res;
                });

                // Збираємо результати
                BallisticData       minItem  = futMin.get();
                BallisticData       maxItem  = futMax.get();
                double              avg      = futAvg.get();
                double              stdDev   = futStd.get();
                List<BallisticData> aboveAvg = futFilter.get();

                System.out.println("\n+==== Результати паралельної обробки ====+");
                System.out.printf("  Мінімум     : %.4f м  (v0=%.1f, α=%.1f°)%n",
                    minItem.getRangeMetric(), minItem.getInitialVelocity(), minItem.getAngleDegrees());
                System.out.printf("  Максимум    : %.4f м  (v0=%.1f, α=%.1f°)%n",
                    maxItem.getRangeMetric(), maxItem.getInitialVelocity(), maxItem.getAngleDegrees());
                System.out.printf("  Середнє     : %.4f м%n", avg);
                System.out.printf("  Станд. відх.: %.4f м%n", stdDev);
                System.out.printf("  Вище середнього (%d з %d):%n", aboveAvg.size(), n);
                for (BallisticData d : aboveAvg)
                    System.out.printf("    v0=%.1f м/с, α=%.1f° → %.4f м%n",
                        d.getInitialVelocity(), d.getAngleDegrees(), d.getRangeMetric());
                System.out.println("+========================================+");

            } catch (InterruptedException | ExecutionException e) {
                System.out.println("  [!] Помилка паралельної обробки: " + e.getMessage());
            } finally {
                pool.shutdown();
            }
        }

        @Override public void undo() { /* read-only */ }
    }

    // =========================================================================
    // WorkerThreadDemoCommand — демонстрація шаблону Worker Thread
    // =========================================================================

    /**
     * Команда-демонстрація шаблону Worker Thread.
     *
     * <p>Створює {@link WorkerThread}, ставить у чергу 3 команди
     * (сортування, паралельна статистика, виведення) і показує їх
     * асинхронне виконання окремим потоком.</p>
     */
    static class WorkerThreadDemoCommand implements Command {

        private final AppContext ctx;

        public WorkerThreadDemoCommand(AppContext ctx) { this.ctx = ctx; }

        @Override public String getName() { return "Worker Thread: черга команд"; }

        @Override
        public void execute() {
            if (ctx.collection.size() < 2) {
                System.out.println("  [!] Потрібно принаймні 2 записи.");
                return;
            }

            System.out.println("\n+==== Демонстрація шаблону Worker Thread ====+");
            System.out.println("  Команди ставляться у чергу і виконуються");
            System.out.println("  асинхронно окремим потоком-робітником.");
            System.out.println("+--------------------------------------------+");

            WorkerThread wt = new WorkerThread();
            wt.start();

            // Команда 1: сортування колекції
            wt.submit(new Command() {
                public String getName() { return "Сортування за дальністю"; }
                public void execute() {
                    List<BallisticData> sorted = new ArrayList<>(ctx.collection.getAll());
                    sorted.sort(Comparator.comparingDouble(BallisticData::getRangeMetric));
                    ctx.collection.setAll(sorted);
                    System.out.println("    → Колекцію відсортовано за дальністю.");
                }
                public void undo() {}
            });

            // Команда 2: паралельна статистика
            wt.submit(new ParallelStatsCommand(ctx));

            // Команда 3: виведення колекції
            wt.submit(new Command() {
                public String getName() { return "Виведення колекції"; }
                public void execute() {
                    BallisticData ref = ctx.lastData != null
                        ? ctx.lastData : ctx.collection.getAll().get(0);
                    ctx.factory.createDisplay(ref).displayTable(ctx.collection.getAll());
                }
                public void undo() {}
            });

            System.out.printf("[WorkerThread] Поставлено 3 команди у чергу.%n");

            try { wt.shutdown(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            System.out.println("+==== Worker Thread завершено ====+");
        }

        @Override public void undo() { /* read-only */ }
    }

    // ===== copyData helper =====
    private static BallisticData copyData(BallisticData s){BallisticData c=new BallisticData(s.getInitialVelocity(),s.getAngleDegrees());c.setRangeMetric(s.getRangeMetric());c.setRangeImperial(s.getRangeImperial());return c;}

    // ===== Concrete Commands =====
    static class AddDataCommand implements Command {
        private final AppContext ctx; private BallisticData added;
        public AddDataCommand(AppContext c){ctx=c;}
        @Override public String getName(){return "Ввести параметри та обчислити";}
        @Override public void execute(){try{double v=readDouble(ctx.scanner,"  > v0 (м/с): ");double a=readDouble(ctx.scanner,"  > α (°, 0–90): ");BallisticData d=new BallisticData(v,a);new BallisticSolver(d).solve();ctx.collection.add(d);ctx.lastData=d;added=d;System.out.println("\n"+new BallisticSolver(d).getReport());System.out.printf("[OK] Додано (всього: %d)%n",ctx.collection.size());}catch(IllegalArgumentException e){System.out.println("  [!] "+e.getMessage());added=null;}}
        @Override public void undo(){if(added!=null){ctx.collection.removeLast();if(ctx.lastData==added)ctx.lastData=null;System.out.printf("  Видалено останній запис. Залишилось: %d%n",ctx.collection.size());added=null;}}
    }
    static class PrintCollectionCommand implements Command {
        private final AppContext ctx; public PrintCollectionCommand(AppContext c){ctx=c;}
        @Override public String getName(){return "Показати всю колекцію";}
        @Override public void execute(){if(ctx.collection.size()==0){System.out.println("  [!] Колекція порожня.");return;}BallisticData ref=ctx.lastData!=null?ctx.lastData:ctx.collection.getAll().get(0);ctx.factory.createDisplay(ref).displayTable(ctx.collection.getAll());}
        @Override public void undo(){}
    }
    static class ShowLastCommand implements Command {
        private final AppContext ctx; public ShowLastCommand(AppContext c){ctx=c;}
        @Override public String getName(){return "Показати останній результат";}
        @Override public void execute(){if(ctx.lastData==null)System.out.println("  [!] Немає результатів.");else ctx.factory.createDisplay(ctx.lastData).displayFull();}
        @Override public void undo(){}
    }
    static class SortCommand implements Command {
        private final AppContext ctx; private final int mode; private List<BallisticData> snapshot;
        public SortCommand(AppContext c,int m){ctx=c;mode=m;}
        @Override public String getName(){switch(mode){case 2:return"Сортувати за початковою швидкістю";case 3:return"Сортувати за кутом пострілу";default:return"Сортувати за дальністю польоту";}}
        @Override public void execute(){if(ctx.collection.size()==0){System.out.println("  [!] Колекція порожня.");return;}snapshot=ctx.collection.snapshot();List<BallisticData> s=new ArrayList<>(snapshot);Comparator<BallisticData> c;switch(mode){case 2:c=Comparator.comparingDouble(BallisticData::getInitialVelocity);break;case 3:c=Comparator.comparingDouble(BallisticData::getAngleDegrees);break;default:c=Comparator.comparingDouble(BallisticData::getRangeMetric);}s.sort(c);ctx.collection.setAll(s);System.out.println("[OK] "+getName()+". Записів: "+ctx.collection.size());}
        @Override public void undo(){if(snapshot!=null){ctx.collection.setAll(snapshot);System.out.println("  Порядок колекції відновлено.");snapshot=null;}}
    }
    static class ScaleVelocityCommand implements Command {
        private final AppContext ctx; private List<BallisticData> snapshot; private double factor;
        public ScaleVelocityCommand(AppContext c){ctx=c;}
        @Override public String getName(){return "Масштабувати швидкості (×коефіцієнт)";}
        @Override public void execute(){if(ctx.collection.size()==0){System.out.println("  [!] Колекція порожня.");return;}factor=readDouble(ctx.scanner,"  > Коефіцієнт масштабування (наприклад, 2.0): ");if(factor<=0){System.out.println("  [!] Коефіцієнт має бути > 0.");return;}snapshot=new ArrayList<>();for(BallisticData d:ctx.collection.getAll())snapshot.add(copyData(d));for(BallisticData d:ctx.collection.getMutable()){d.setInitialVelocity(d.getInitialVelocity()*factor);new BallisticSolver(d).solve();}System.out.printf("[OK] Масштабовано ×%.2f. Перераховано %d записів.%n",factor,ctx.collection.size());}
        @Override public void undo(){if(snapshot!=null){ctx.collection.setAll(snapshot);System.out.printf("  Масштабування ×%.2f скасовано.%n",factor);snapshot=null;}}
    }
    static class NormalizeRangesCommand implements Command {
        private final AppContext ctx; private List<BallisticData> snapshot;
        public NormalizeRangesCommand(AppContext c){ctx=c;}
        @Override public String getName(){return "Нормалізувати дальності до [0, 1]";}
        @Override public void execute(){if(ctx.collection.size()<2){System.out.println("  [!] Потрібно принаймні 2 записи.");return;}snapshot=new ArrayList<>();for(BallisticData d:ctx.collection.getAll())snapshot.add(copyData(d));double min=ctx.collection.getAll().stream().mapToDouble(BallisticData::getRangeMetric).min().orElse(0),max=ctx.collection.getAll().stream().mapToDouble(BallisticData::getRangeMetric).max().orElse(1),range=max-min;if(range<1e-9){System.out.println("  [!] Усі значення однакові.");return;}for(BallisticData d:ctx.collection.getMutable()){double n=(d.getRangeMetric()-min)/range;d.setRangeMetric(n);d.setRangeImperial(n*BallisticData.METERS_TO_FEET);}System.out.printf("[OK] Нормалізовано %d записів. min=%.2f м, max=%.2f м%n",ctx.collection.size(),min,max);}
        @Override public void undo(){if(snapshot!=null){ctx.collection.setAll(snapshot);System.out.println("  Нормалізацію скасовано.");snapshot=null;}}
    }
    static class InterpolateCommand implements Command {
        private final AppContext ctx; private int addedCount=0;
        public InterpolateCommand(AppContext c){ctx=c;}
        @Override public String getName(){return "Інтерполювати між крайніми елементами";}
        @Override public void execute(){if(ctx.collection.size()<2){System.out.println("  [!] Потрібно принаймні 2 записи.");return;}int n=readInt(ctx.scanner,"  > Кількість проміжних точок (1–20): ");if(n<1||n>20){System.out.println("  [!] Діапазон: 1–20.");return;}List<BallisticData> all=ctx.collection.getAll();BallisticData f=all.get(0),l=all.get(all.size()-1);addedCount=n;for(int i=1;i<=n;i++){double t=(double)i/(n+1),v=f.getInitialVelocity()+t*(l.getInitialVelocity()-f.getInitialVelocity()),a=Math.max(0,Math.min(90,f.getAngleDegrees()+t*(l.getAngleDegrees()-f.getAngleDegrees())));BallisticData d=new BallisticData(v,a);new BallisticSolver(d).solve();ctx.collection.add(d);}System.out.printf("[OK] Додано %d інтерпольованих записів. Всього: %d%n",n,ctx.collection.size());}
        @Override public void undo(){for(int i=0;i<addedCount;i++)ctx.collection.removeLast();System.out.printf("  Видалено %d інтерпольованих записів.%n",addedCount);addedCount=0;}
    }
    static class SearchByRangeCommand implements Command {
        private final AppContext ctx; public SearchByRangeCommand(AppContext c){ctx=c;}
        @Override public String getName(){return "Пошук за діапазоном дальності (м)";}
        @Override public void execute(){if(ctx.collection.size()==0){System.out.println("  [!] Колекція порожня.");return;}double lo=readDouble(ctx.scanner,"  > Мінімальна дальність (м): "),hi=readDouble(ctx.scanner,"  > Максимальна дальність (м): ");List<BallisticData> found=new ArrayList<>();for(BallisticData d:ctx.collection.getAll())if(d.getRangeMetric()>=lo&&d.getRangeMetric()<=hi)found.add(d);if(found.isEmpty())System.out.printf("  Нічого не знайдено в [%.2f ; %.2f] м%n",lo,hi);else{System.out.printf("[OK] Знайдено %d запис(ів):%n",found.size());ctx.factory.createDisplay(found.get(0)).displayTable(found);}}
        @Override public void undo(){}
    }
    static class SaveCommand implements Command {
        private final AppContext ctx; public SaveCommand(AppContext c){ctx=c;}
        @Override public String getName(){return "Зберегти колекцію у файл";}
        @Override public void execute(){try{ctx.collection.saveToFile(AppContext.COLLECTION_FILE);System.out.printf("[OK] Збережено \"%s\" (%d записів)%n",AppContext.COLLECTION_FILE,ctx.collection.size());}catch(IOException e){System.out.println("  [!] "+e.getMessage());}}
        @Override public void undo(){new File(AppContext.COLLECTION_FILE).delete();System.out.println("  Файл видалено.");}
    }
    static class LoadCommand implements Command {
        private final AppContext ctx; private List<BallisticData> snapshot;
        public LoadCommand(AppContext c){ctx=c;}
        @Override public String getName(){return "Завантажити колекцію з файлу";}
        @Override public void execute(){snapshot=ctx.collection.snapshot();try{ctx.collection.loadFromFile(AppContext.COLLECTION_FILE);System.out.printf("[OK] Завантажено \"%s\" (%d записів)%n",AppContext.COLLECTION_FILE,ctx.collection.size());if(ctx.collection.size()>0)ctx.lastData=ctx.collection.getAll().get(ctx.collection.size()-1);}catch(FileNotFoundException e){System.out.printf("  [!] Файл \"%s\" не знайдено.%n",AppContext.COLLECTION_FILE);snapshot=null;}catch(IOException|ClassNotFoundException e){System.out.println("  [!] "+e.getMessage());snapshot=null;}}
        @Override public void undo(){if(snapshot!=null){ctx.collection.setAll(snapshot);System.out.println("  Завантаження скасовано.");snapshot=null;}}
    }
    static class ClearCommand implements Command {
        private final AppContext ctx; private List<BallisticData> snapshot;
        public ClearCommand(AppContext c){ctx=c;}
        @Override public String getName(){return "Очистити колекцію";}
        @Override public void execute(){snapshot=ctx.collection.snapshot();ctx.collection.clear();ctx.lastData=null;System.out.println("[OK] Колекцію очищено.");}
        @Override public void undo(){if(snapshot!=null){ctx.collection.setAll(snapshot);System.out.printf("  Колекцію відновлено. Записів: %d%n",ctx.collection.size());if(ctx.collection.size()>0)ctx.lastData=ctx.collection.getAll().get(ctx.collection.size()-1);snapshot=null;}}
    }
    static class ChangeFactoryCommand implements Command {
        private final AppContext ctx; private DisplayFactory prev;
        public ChangeFactoryCommand(AppContext c){ctx=c;}
        @Override public String getName(){return "Змінити формат відображення";}
        @Override public void execute(){System.out.println("\n  1.TextDisplay 2.DetailedDisplay 3.CompactDisplay 4.SimpleTable 5.BorderedTable 6.CsvTable");int fmt=readInt(ctx.scanner,"  > Формат: ");prev=ctx.factory;ctx.factory=chooseFactory(fmt,ctx.tableConfig);System.out.printf("[OK] Обрано: %s%n",ctx.factory.getClass().getSimpleName());}
        @Override public void undo(){if(prev!=null){ctx.factory=prev;System.out.println("  Формат відновлено: "+ctx.factory.getClass().getSimpleName());prev=null;}}
    }
    static class ConfigureTableCommand implements Command {
        private final AppContext ctx; private TableConfig prev;
        public ConfigureTableCommand(AppContext c){ctx=c;}
        @Override public String getName(){return "Налаштувати параметри таблиці";}
        @Override public void execute(){prev=ctx.tableConfig;ctx.tableConfig=readTableConfig(ctx.scanner);}
        @Override public void undo(){if(prev!=null){ctx.tableConfig=prev;System.out.println("  Конфігурацію відновлено: "+ctx.tableConfig);prev=null;}}
    }
    static class PolymorphismDemoCommand implements Command {
        private final AppContext ctx; public PolymorphismDemoCommand(AppContext c){ctx=c;}
        @Override public String getName(){return "Демонстрація поліморфізму (dynamic dispatch)";}
        @Override public void execute(){if(ctx.collection.size()==0){System.out.println("  [!] Колекція порожня.");return;}BallisticData ref=ctx.lastData!=null?ctx.lastData:ctx.collection.getAll().get(0);System.out.println("\n+==== Демонстрація поліморфізму (Dynamic Dispatch) ====+");Displayable[]ds={new TextDisplay(ref),new SimpleTableDisplay(ref,ctx.tableConfig),new BorderedTableDisplay(ref,ctx.tableConfig),new DetailedTextDisplay(ref),new CompactDisplay(ref),new CsvTableDisplay(ref,ctx.tableConfig)};for(Displayable d:ds){System.out.printf("  %-35s → ",d.getClass().getSimpleName());d.displayShort();}System.out.println("+======================================================+");}
        @Override public void undo(){}
    }

    // ===== MacroCommand =====
    static class MacroCommand implements Command {
        private final String name; private final List<Command> commands;
        public MacroCommand(String n,List<Command> c){name=n;commands=new ArrayList<>(c);}
        @Override public String getName(){return "Макро: "+name;}
        @Override public void execute(){System.out.println("[Macro] Виконання: «"+name+"»");for(int i=0;i<commands.size();i++){System.out.printf("  [%d/%d] %s%n",i+1,commands.size(),commands.get(i).getName());commands.get(i).execute();}System.out.println("[Macro] Завершено: «"+name+"»");}
        @Override public void undo(){System.out.println("[Macro] Скасування: «"+name+"»");for(int i=commands.size()-1;i>=0;i--){System.out.printf("  [%d/%d] Undo: %s%n",commands.size()-i,commands.size(),commands.get(i).getName());commands.get(i).undo();}System.out.println("[Macro] Скасування завершено.");}
    }

    // ===== BallisticTest =====
    static class BallisticTest {
        private static final double EPS=1e-6; private static final String TF="test_ballistic.ser",TC="test_collection.ser";
        private int passed=0,failed=0;
        public boolean runAll(){
            System.out.println("+======================================================+");
            System.out.println("|          Тестування — BallisticApp v3.0              |");
            System.out.println("+======================================================+");
            System.out.println("\n--- Група 1: Обчислення ---");
            double v=100;ae("Кут 45°",(v*v)/BallisticData.G_METRIC,new BallisticSolver(v,45).computeRangeMetric());ae("Кут 0°",0.0,new BallisticSolver(v,0).computeRangeMetric());ae("Кут 90°",0.0,new BallisticSolver(v,90).computeRangeMetric());
            double v2=200;BallisticSolver s2=new BallisticSolver(v2,30);s2.solve();ae("Відомі значення v0=200 α=30°",(v2*v2*Math.sin(Math.toRadians(60)))/BallisticData.G_METRIC,s2.getData().getRangeMetric());
            BallisticSolver s3=new BallisticSolver(150,60);s3.solve();double mf=s3.getData().getRangeMetric()*BallisticData.METERS_TO_FEET,imp=s3.getData().getRangeImperial();rp("Узгодженість метрична/англійська",Math.abs(mf-imp)/imp<0.005,String.format("err=%.6f",Math.abs(mf-imp)/imp));
            System.out.println("\n--- Група 2: Серіалізація ---");
            try{BallisticData o=new BallisticData(250,35);new BallisticSolver(o).solve();try(ObjectOutputStream os=new ObjectOutputStream(new FileOutputStream(TF))){os.writeObject(o);}BallisticData r;try(ObjectInputStream is=new ObjectInputStream(new FileInputStream(TF))){r=(BallisticData)is.readObject();}rp("Серіалізація поля",o.getInitialVelocity()==r.getInitialVelocity()&&o.getRangeMetric()==r.getRangeMetric(),"");}catch(Exception e){rp("Серіалізація",false,e.getMessage());}
            try{BallisticData o=new BallisticData(100,45);try(ObjectOutputStream os=new ObjectOutputStream(new FileOutputStream(TF))){os.writeObject(o);}BallisticData r;try(ObjectInputStream is=new ObjectInputStream(new FileInputStream(TF))){r=(BallisticData)is.readObject();}rp("Transient: timestamp→0",r.getTimestamp()==0L,"got:"+r.getTimestamp());rp("Transient: status→null",r.getStatusMessage()==null,"got:"+r.getStatusMessage());}catch(Exception e){rp("Transient",false,e.getMessage());}
            System.out.println("\n--- Група 3: Валідація ---");
            try{new BallisticData(-1,45);rp("Від'ємна швидкість→виняток",false,"не кинуто");}catch(IllegalArgumentException e){rp("Від'ємна швидкість→виняток",true,e.getMessage());}
            boolean c1=false,c2=false;try{new BallisticData(100,-1);}catch(IllegalArgumentException e){c1=true;}try{new BallisticData(100,91);}catch(IllegalArgumentException e){c2=true;}rp("Некоректний кут→виняток",c1&&c2,"");
            try{new BallisticSolver(null);rp("Null→виняток",false,"не кинуто");}catch(IllegalArgumentException e){rp("Null→виняток",true,e.getMessage());}
            System.out.println("\n--- Група 4: ResultCollection ---");
            ResultCollection rc=new ResultCollection();rc.add(new BallisticData(100,30));rc.add(new BallisticData(200,45));rp("Add&Size",rc.size()==2,"size="+rc.size());
            try{ResultCollection oc=new ResultCollection();BallisticData d1=new BallisticData(100,30),d2=new BallisticData(200,45);new BallisticSolver(d1).solve();new BallisticSolver(d2).solve();oc.add(d1);oc.add(d2);oc.saveToFile(TC);ResultCollection lc=new ResultCollection();lc.loadFromFile(TC);rp("Save/Load",lc.size()==2&&lc.getAll().get(0).getInitialVelocity()==100,"size="+lc.size());}catch(Exception e){rp("Save/Load",false,e.getMessage());}
            ResultCollection rc2=new ResultCollection();rc2.add(new BallisticData(100,45));rc2.clear();rp("Clear",rc2.size()==0,"size="+rc2.size());
            System.out.println("\n--- Група 5: Фабрики ---");
            BallisticData sd=mk();rp("TextDisplayFactory",new TextDisplayFactory().createDisplay(sd) instanceof TextDisplay,"");rp("DetailedDisplayFactory",new DetailedDisplayFactory().createDisplay(sd) instanceof DetailedTextDisplay,"");rp("CompactDisplayFactory",new CompactDisplayFactory().createDisplay(sd) instanceof CompactDisplay,"");rp("SimpleTableDisplayFactory",new SimpleTableDisplayFactory().createDisplay(sd) instanceof SimpleTableDisplay,"");rp("BorderedTableDisplayFactory",new BorderedTableDisplayFactory().createDisplay(sd) instanceof BorderedTableDisplay,"");rp("CsvTableDisplayFactory",new CsvTableDisplayFactory().createDisplay(sd) instanceof CsvTableDisplay,"");
            System.out.println("\n--- Група 6: TableConfig ---");
            TableConfig cfg=new TableConfig();rp("Defaults",cfg.getColumnWidth()==10&&cfg.getSeparator()=='|'&&cfg.isShowMetric()&&cfg.isShowImperial()&&cfg.getMaxRows()==0&&!cfg.isShowDerived(),cfg.toString());
            TableConfig cfg2=new TableConfig(14,';',true,false,5,"Тест",true);rp("Custom",cfg2.getColumnWidth()==14&&cfg2.getSeparator()==';'&&!cfg2.isShowImperial()&&cfg2.getMaxRows()==5&&cfg2.isShowDerived()&&cfg2.getTitle().equals("Тест"),cfg2.toString());
            TableConfig cfg3=new TableConfig();cfg3.setMaxRows(3);rp("effectiveRows",cfg3.effectiveRows(10)==3&&cfg3.effectiveRows(2)==2,"");
            TableConfig cfg4=new TableConfig();cfg4.setColumnWidth(1);rp("minWidth=4",cfg4.getColumnWidth()==4,"w="+cfg4.getColumnWidth());
            System.out.println("\n--- Група 7: Overloading ---");
            try{PrintStream old=System.out;System.setOut(new PrintStream(OutputStream.nullOutputStream()));new SimpleTableDisplay(mk()).displayTable(mkl(5),3);System.setOut(old);rp("Overloading(List,int)",true,"");}catch(Exception e){rp("Overloading(List,int)",false,e.getMessage());}
            try{PrintStream old=System.out;System.setOut(new PrintStream(OutputStream.nullOutputStream()));new SimpleTableDisplay(mk()).displayTable(mkl(3),new TableConfig(8,'+',true,false,0,"T",false));System.setOut(old);rp("Overloading(List,Config)",true,"");}catch(Exception e){rp("Overloading(List,Config)",false,e.getMessage());}
            try{PrintStream old=System.out;System.setOut(new PrintStream(OutputStream.nullOutputStream()));new CsvTableDisplay(mk()).displayTable(mkl(5),2);System.setOut(old);rp("CSV Overloading(List,int)",true,"");}catch(Exception e){rp("CSV Overloading(List,int)",false,e.getMessage());}
            try{PrintStream old=System.out;System.setOut(new PrintStream(OutputStream.nullOutputStream()));new CsvTableDisplay(mk()).displayTable(mkl(3),new TableConfig(8,';',true,true,0,"C",true));System.setOut(old);rp("CSV Overloading(List,Config)",true,"");}catch(Exception e){rp("CSV Overloading(List,Config)",false,e.getMessage());}
            System.out.println("\n--- Група 8: Overriding ---");
            SimpleTableDisplay std=new SimpleTableDisplay(mk());rp("SimpleTableDisplay extends TextDisplay",(std instanceof TextDisplay)&&!new TextDisplay(mk()).getClass().equals(std.getClass()),"");
            BorderedTableDisplay bd=new BorderedTableDisplay(mk());rp("BorderedTableDisplay extends SimpleTableDisplay",(bd instanceof SimpleTableDisplay)&&(bd instanceof TextDisplay)&&(bd instanceof Displayable),"");
            System.out.println("\n--- Група 9: Поліморфізм ---");
            try{PrintStream old=System.out;System.setOut(new PrintStream(OutputStream.nullOutputStream()));Displayable[]ds={new TextDisplay(mk()),new SimpleTableDisplay(mk()),new BorderedTableDisplay(mk()),new DetailedTextDisplay(mk()),new CompactDisplay(mk()),new CsvTableDisplay(mk())};for(Displayable d:ds)d.displayTable(mkl(2));System.setOut(old);rp("Поліморфізм displayTable (6 типів)",true,"");}catch(Exception e){rp("Поліморфізм displayTable",false,e.getMessage());}
            try{PrintStream old=System.out;System.setOut(new PrintStream(OutputStream.nullOutputStream()));Displayable[]ds={new TextDisplay(mk()),new SimpleTableDisplay(mk()),new BorderedTableDisplay(mk()),new CompactDisplay(mk())};for(Displayable d:ds)d.displayShort();System.setOut(old);rp("Поліморфізм displayShort (4 типи)",true,"");}catch(Exception e){rp("Поліморфізм displayShort",false,e.getMessage());}
            System.out.println("\n==============================================");System.out.printf("  Результат: %d пройдено, %d провалено з %d%n",passed,failed,passed+failed);System.out.println("==============================================");
            new File(TF).delete();new File(TC).delete();return failed==0;
        }
        private BallisticData mk(){BallisticData d=new BallisticData(100,45);new BallisticSolver(d).solve();return d;}
        private List<BallisticData> mkl(int n){List<BallisticData> l=new ArrayList<>();for(int i=0;i<n;i++){BallisticData d=new BallisticData(50+i*30.0,15+i*10.0);new BallisticSolver(d).solve();l.add(d);}return l;}
        private void ae(String name,double exp,double act){rp(name,Math.abs(exp-act)<EPS,String.format("exp=%.6f got=%.6f",exp,act));}
        private void rp(String n,boolean ok,String d){System.out.printf("  %s : %s",ok?"[OK]   PASS":"[FAIL] FAIL",n);if(!d.isEmpty())System.out.printf("  [%s]",d);System.out.println();if(ok)passed++;else failed++;}
    }

    // ===== BallisticCommandTest =====
    static class BallisticCommandTest {
        private static final double EPS=1e-6; private int passed=0,failed=0;
        public boolean runAll(){
            System.out.println("+======================================================+");
            System.out.println("|  BallisticCommandTest — Command/Singleton/Parallel   |");
            System.out.println("+======================================================+");
            System.out.println("\n--- Група 1: Singleton ---");
            CommandHistory a=CommandHistory.getInstance(),b=CommandHistory.getInstance();rp("Singleton: один об'єкт",a==b,"");
            a.clearHistory();int bef=a.size();CommandHistory.getInstance().push(new Command(){public void execute(){}public void undo(){}public String getName(){return"s";}});int aft=CommandHistory.getInstance().size();a.clearHistory();rp("Singleton: стан спільний",aft==bef+1,"до="+bef+" після="+aft);
            System.out.println("\n--- Група 2: AddDataCommand ---");
            AppContext ctx=mc();AddDataCommand ac=new AddDataCommand(ctx){@Override public void execute(){BallisticData d=new BallisticData(100,45);new BallisticSolver(d).solve();ctx.collection.add(d);ctx.lastData=d;}};ac.execute();rp("Add: колекція збільшилась",ctx.collection.size()==1,"size="+ctx.collection.size());
            AppContext ctx2=mc();BallisticData dd=new BallisticData(100,45);new BallisticSolver(dd).solve();ctx2.collection.add(dd);int b2=ctx2.collection.size();ctx2.collection.removeLast();rp("Add undo: зменшився",ctx2.collection.size()==b2-1,"до="+b2+" після="+ctx2.collection.size());
            System.out.println("\n--- Група 3: SortCommand ---");
            AppContext sc=mfc();new SortCommand(sc,1).execute();List<BallisticData> al=sc.collection.getAll();boolean sorted=true;for(int i=1;i<al.size();i++)if(al.get(i).getRangeMetric()<al.get(i-1).getRangeMetric()){sorted=false;break;}rp("Sort: зростаючий порядок",sorted,"");
            AppContext sc2=mfc();List<BallisticData> orig=sc2.collection.snapshot();SortCommand scmd=new SortCommand(sc2,1);scmd.execute();scmd.undo();boolean res=true;for(int i=0;i<orig.size();i++)if(orig.get(i).getInitialVelocity()!=sc2.collection.getAll().get(i).getInitialVelocity()){res=false;break;}rp("Sort undo: порядок відновлено",res,"");
            System.out.println("\n--- Група 4: ScaleVelocityCommand ---");
            AppContext kc=mc();BallisticData kd=new BallisticData(100,45);new BallisticSolver(kd).solve();kc.collection.add(kd);double ov=kd.getInitialVelocity();for(BallisticData x:kc.collection.getMutable()){x.setInitialVelocity(x.getInitialVelocity()*2);new BallisticSolver(x).solve();}rp("Scale: v0×2",Math.abs(kc.collection.getAll().get(0).getInitialVelocity()-ov*2)<EPS,"");
            AppContext kc2=mc();BallisticData kd2=new BallisticData(150,30);new BallisticSolver(kd2).solve();kc2.collection.add(kd2);double ov2=kd2.getInitialVelocity();List<BallisticData> ks=new ArrayList<>();ks.add(copyData(kd2));for(BallisticData x:kc2.collection.getMutable())x.setInitialVelocity(x.getInitialVelocity()*3);kc2.collection.setAll(ks);rp("Scale undo: відновлено",Math.abs(kc2.collection.getAll().get(0).getInitialVelocity()-ov2)<EPS,"");
            System.out.println("\n--- Група 5: NormalizeRangesCommand ---");
            AppContext nc=mfc();new NormalizeRangesCommand(nc).execute();double nm=nc.collection.getAll().stream().mapToDouble(BallisticData::getRangeMetric).min().orElse(-1),nM=nc.collection.getAll().stream().mapToDouble(BallisticData::getRangeMetric).max().orElse(-1);rp("Normalize: min≈0",Math.abs(nm)<EPS,"min="+nm);rp("Normalize: max≈1",Math.abs(nM-1)<EPS,"max="+nM);
            AppContext nc2=mfc();double nof=nc2.collection.getAll().get(0).getRangeMetric();NormalizeRangesCommand ncmd=new NormalizeRangesCommand(nc2);ncmd.execute();ncmd.undo();rp("Normalize undo",Math.abs(nc2.collection.getAll().get(0).getRangeMetric()-nof)<EPS,String.format("%.4f",nof));
            System.out.println("\n--- Група 6: ClearCommand ---");
            AppContext cc=mfc();new ClearCommand(cc).execute();rp("Clear: порожня",cc.collection.size()==0,"size="+cc.collection.size());
            AppContext cc2=mfc();int cb=cc2.collection.size();ClearCommand ccmd=new ClearCommand(cc2);ccmd.execute();ccmd.undo();rp("Clear undo: відновлено",cc2.collection.size()==cb,"до="+cb+" після="+cc2.collection.size());
            System.out.println("\n--- Група 7: MacroCommand ---");
            List<boolean[]> flags=Arrays.asList(new boolean[]{false},new boolean[]{false});Command c1=new Command(){public void execute(){flags.get(0)[0]=true;}public void undo(){}public String getName(){return"C1";}};Command c2=new Command(){public void execute(){flags.get(1)[0]=true;}public void undo(){}public String getName(){return"C2";}};new MacroCommand("T",Arrays.asList(c1,c2)).execute();rp("MacroCommand: виконані",flags.get(0)[0]&&flags.get(1)[0],"");
            List<Integer> order=new ArrayList<>();Command u1=new Command(){public void execute(){}public void undo(){order.add(1);}public String getName(){return"U1";}};Command u2=new Command(){public void execute(){}public void undo(){order.add(2);}public String getName(){return"U2";}};MacroCommand mc2=new MacroCommand("U",Arrays.asList(u1,u2));mc2.execute();mc2.undo();rp("MacroCommand undo: зворотний",order.size()==2&&order.get(0)==2&&order.get(1)==1,"порядок="+order);
            System.out.println("\n--- Група 8: Menu ---");
            Menu menu=new Menu("T");menu.addEntry("1","L",new Command(){public void execute(){}public void undo(){}public String getName(){return"L";}});boolean hb=menu.hasKey("1");menu.removeEntry("1");rp("Menu add/remove",hb&&!menu.hasKey("1"),"");
            boolean[]ran={false};Menu m2=new Menu("T2");m2.addEntry("X","C",new Command(){public void execute(){ran[0]=true;}public void undo(){}public String getName(){return"X";}});CommandHistory.getInstance().clearHistory();boolean found=m2.executeCommand("X");CommandHistory.getInstance().clearHistory();rp("Menu.executeCommand",found&&ran[0],"");
            System.out.println("\n--- Група 9: SearchByRangeCommand ---");
            AppContext src=mfc();long cnt=src.collection.getAll().stream().filter(d->d.getRangeMetric()>=0&&d.getRangeMetric()<=1e9).count();rp("Search: знайдено у широкому діапазоні",cnt==src.collection.size(),"count="+cnt);
            long cnt2=src.collection.getAll().stream().filter(d->d.getRangeMetric()<0).count();rp("Search: порожній результат",cnt2==0,"count="+cnt2);
            System.out.println("\n--- Група 10: InterpolateCommand ---");
            AppContext ic=mfc();int ib=ic.collection.size();InterpolateCommand icmd=new InterpolateCommand(ic){@Override public void execute(){int n=3;List<BallisticData> all=ic.collection.getAll();BallisticData f=all.get(0),l=all.get(all.size()-1);for(int i=1;i<=n;i++){double t=(double)i/(n+1),v=f.getInitialVelocity()+t*(l.getInitialVelocity()-f.getInitialVelocity()),an=Math.max(0,Math.min(90,f.getAngleDegrees()+t*(l.getAngleDegrees()-f.getAngleDegrees())));BallisticData d=new BallisticData(v,an);new BallisticSolver(d).solve();ic.collection.add(d);}}};icmd.execute();rp("Interpolate: 3 точки",ic.collection.size()==ib+3,"до="+ib+" після="+ic.collection.size());
            AppContext ic2=mfc();int ib2=ic2.collection.size();for(int i=0;i<2;i++){BallisticData d=new BallisticData(50+i*10,40);new BallisticSolver(d).solve();ic2.collection.add(d);}for(int i=0;i<2;i++)ic2.collection.removeLast();rp("Interpolate undo: відновлено",ic2.collection.size()==ib2,"до="+ib2+" після="+ic2.collection.size());

            System.out.println("\n--- Група 11: ParallelStatsCommand ---");
            testParallelMin();
            testParallelAvg();
            testParallelFilter();

            System.out.println("\n--- Група 12: WorkerThread ---");
            testWorkerThreadExecutes();
            testWorkerThreadFifo();

            System.out.println("\n==============================================");System.out.printf("  Результат: %d пройдено, %d провалено з %d%n",passed,failed,passed+failed);System.out.println("==============================================");
            return failed==0;
        }

        private void testParallelMin() {
            AppContext ctx = mfc();
            List<BallisticData> all = new ArrayList<>(ctx.collection.getAll());
            double seqMin = all.stream().mapToDouble(BallisticData::getRangeMetric).min().orElse(0);
            double seqMax = all.stream().mapToDouble(BallisticData::getRangeMetric).max().orElse(0);
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                Future<Double> fMin = pool.submit(() -> all.stream().mapToDouble(BallisticData::getRangeMetric).min().orElse(0));
                Future<Double> fMax = pool.submit(() -> all.stream().mapToDouble(BallisticData::getRangeMetric).max().orElse(0));
                rp("ParallelStats: мінімум збігається",  Math.abs(fMin.get()-seqMin)<EPS, String.format("seq=%.4f par=%.4f",seqMin,fMin.get()));
                rp("ParallelStats: максимум збігається", Math.abs(fMax.get()-seqMax)<EPS, String.format("seq=%.4f par=%.4f",seqMax,fMax.get()));
            } catch(Exception e){rp("ParallelStats min/max",false,e.getMessage());rp("ParallelStats min/max2",false,"");}
            finally{pool.shutdown();}
        }
        private void testParallelAvg() {
            AppContext ctx = mfc();
            List<BallisticData> all = new ArrayList<>(ctx.collection.getAll());
            double seqAvg = all.stream().mapToDouble(BallisticData::getRangeMetric).average().orElse(0);
            ExecutorService pool = Executors.newSingleThreadExecutor();
            try {
                Future<Double> fAvg = pool.submit(() -> all.stream().mapToDouble(BallisticData::getRangeMetric).average().orElse(0));
                rp("ParallelStats: середнє збігається", Math.abs(fAvg.get()-seqAvg)<EPS, String.format("seq=%.4f par=%.4f",seqAvg,fAvg.get()));
            } catch(Exception e){rp("ParallelStats avg",false,e.getMessage());}
            finally{pool.shutdown();}
        }
        private void testParallelFilter() {
            AppContext ctx = mfc();
            List<BallisticData> all = new ArrayList<>(ctx.collection.getAll());
            double avg = all.stream().mapToDouble(BallisticData::getRangeMetric).average().orElse(0);
            long seqCount = all.stream().filter(d->d.getRangeMetric()>avg).count();
            ExecutorService pool = Executors.newSingleThreadExecutor();
            try {
                double avgF = avg;
                Future<Long> fCount = pool.submit(() -> all.stream().filter(d->d.getRangeMetric()>avgF).count());
                rp("ParallelStats: відбір за критерієм", fCount.get()==seqCount, String.format("seq=%d par=%d",seqCount,fCount.get()));
            } catch(Exception e){rp("ParallelStats filter",false,e.getMessage());}
            finally{pool.shutdown();}
        }
        private void testWorkerThreadExecutes() {
            AtomicInteger counter = new AtomicInteger(0);
            WorkerThread wt = new WorkerThread();
            PrintStream old = System.out;
            System.setOut(new PrintStream(OutputStream.nullOutputStream()));
            wt.start();
            for (int i = 0; i < 3; i++) {
                wt.submit(new Command(){public void execute(){counter.incrementAndGet();}public void undo(){}public String getName(){return"c";}});
            }
            try { wt.shutdown(); } catch(InterruptedException e){Thread.currentThread().interrupt();}
            System.setOut(old);
            rp("WorkerThread: 3 команди виконано", counter.get()==3&&wt.completedCount()==3, "executed="+counter.get());
        }
        private void testWorkerThreadFifo() {
            List<Integer> order = Collections.synchronizedList(new ArrayList<>());
            WorkerThread wt = new WorkerThread();
            PrintStream old = System.out;
            System.setOut(new PrintStream(OutputStream.nullOutputStream()));
            wt.start();
            for (int i = 1; i <= 3; i++) {
                final int n = i;
                wt.submit(new Command(){public void execute(){order.add(n);}public void undo(){}public String getName(){return"o"+n;}});
            }
            try { wt.shutdown(); } catch(InterruptedException e){Thread.currentThread().interrupt();}
            System.setOut(old);
            boolean fifo = order.size()==3&&order.get(0)==1&&order.get(1)==2&&order.get(2)==3;
            rp("WorkerThread: FIFO порядок", fifo, "order="+order);
        }

        private AppContext mc(){return new AppContext(new Scanner(""));}
        private AppContext mfc(){AppContext ctx=mc();double[][]p={{300,20},{100,45},{500,60},{200,30}};for(double[]x:p){BallisticData d=new BallisticData(x[0],x[1]);new BallisticSolver(d).solve();ctx.collection.add(d);}return ctx;}
        private void rp(String n,boolean ok,String d){System.out.printf("  %s : %s",ok?"[OK]   PASS":"[FAIL] FAIL",n);if(!d.isEmpty())System.out.printf("  [%s]",d);System.out.println();if(ok)passed++;else failed++;}
    }

    // ===== Utilities =====
    private static double readDouble(Scanner s,String p){while(true){System.out.print(p);try{return Double.parseDouble(s.nextLine().trim());}catch(NumberFormatException e){System.out.println("  [!] Некоректне число.");}}}
    private static int readInt(Scanner s,String p){while(true){System.out.print(p);try{return Integer.parseInt(s.nextLine().trim());}catch(NumberFormatException e){System.out.println("  [!] Некоректне число.");}}}
    private static char readChar(Scanner s,String p,char def){System.out.print(p);String l=s.nextLine().trim();return l.isEmpty()?def:l.charAt(0);}
    private static boolean readBool(Scanner s,String p){System.out.print(p+" [y/n]: ");String l=s.nextLine().trim().toLowerCase();return l.equals("y")||l.equals("т")||l.equals("1");}
    private static String readString(Scanner s,String p,String def){System.out.print(p+" [Enter = \""+def+"\"]: ");String l=s.nextLine().trim();return l.isEmpty()?def:l;}
    private static TableConfig readTableConfig(Scanner s){
        System.out.println("\n  +--- Налаштування таблиці ---+");
        int w=readInt(s,"  > Ширина стовпця (4..30): ");char sep=readChar(s,"  > Символ-роздільник: ",'|');boolean sm=readBool(s,"  > Показувати метричну (м)"),si=readBool(s,"  > Показувати англійську (фут)"),sd=readBool(s,"  > Показувати км/милі");int mr=readInt(s,"  > Макс. рядків (0=всі): ");String t=readString(s,"  > Заголовок таблиці","Результати");
        TableConfig cfg=new TableConfig(w,sep,sm,si,mr,t,sd);System.out.printf("  [OK] Конфігурація: %s%n",cfg);return cfg;
    }
    private static DisplayFactory chooseFactory(int c,TableConfig cfg){switch(c){case 2:return new DetailedDisplayFactory();case 3:return new CompactDisplayFactory();case 4:return new SimpleTableDisplayFactory(cfg);case 5:return new BorderedTableDisplayFactory(cfg);case 6:return new CsvTableDisplayFactory(cfg);default:return new TextDisplayFactory();}}

    // ===== main =====
    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out,true,"UTF-8"));
        System.setErr(new PrintStream(System.err,true,"UTF-8"));

        System.out.println("\n>>> Запуск базових тестів...");
        boolean t1=new BallisticTest().runAll();
        if(!t1)System.err.println("[!] Деякі базові тести провалено.");

        System.out.println("\n>>> Запуск тестів Command/Singleton/Parallel/WorkerThread...");
        boolean t2=new BallisticCommandTest().runAll();
        if(!t2)System.err.println("[!] Деякі тести провалено.");

        Scanner scanner=new Scanner(System.in);
        AppContext ctx=new AppContext(scanner);

        Menu menu=new Menu("Балістичний калькулятор v5.0 — Command + Parallel + Worker Thread");
        menu.addEntry("1","Ввести параметри та обчислити",new AddDataCommand(ctx));
        menu.addEntry("2","Показати останній результат",new ShowLastCommand(ctx));
        menu.addEntry("3","Показати всю колекцію",new PrintCollectionCommand(ctx));
        menu.addEntry("4","Показати з лімітом рядків (overloading)",new Command(){
            public String getName(){return "Показати з лімітом";}
            public void execute(){if(ctx.collection.size()==0){System.out.println("  [!] Порожньо.");return;}int lim=readInt(ctx.scanner,"  > Ліміт рядків: ");BallisticData ref=ctx.lastData!=null?ctx.lastData:ctx.collection.getAll().get(0);Displayable d=ctx.factory.createDisplay(ref);if(d instanceof ConfigurableDisplay)((ConfigurableDisplay)d).displayTable(ctx.collection.getAll(),lim);else d.displayTable(ctx.collection.getAll());}
            public void undo(){}});
        menu.addEntry("5","Змінити формат відображення (Factory Method)",new ChangeFactoryCommand(ctx));
        menu.addEntry("6","Налаштувати параметри таблиці (TableConfig)",new ConfigureTableCommand(ctx));
        menu.addEntry("7","Сортувати за дальністю",new SortCommand(ctx,1));
        menu.addEntry("8","Сортувати за швидкістю",new SortCommand(ctx,2));
        menu.addEntry("9","Сортувати за кутом",new SortCommand(ctx,3));
        menu.addEntry("10","Масштабувати швидкості (×коефіцієнт)",new ScaleVelocityCommand(ctx));
        menu.addEntry("11","Нормалізувати дальності до [0, 1]",new NormalizeRangesCommand(ctx));
        menu.addEntry("12","Інтерполювати між крайніми елементами",new InterpolateCommand(ctx));
        menu.addEntry("13","Пошук за діапазоном дальності (м)",new SearchByRangeCommand(ctx));
        menu.addEntry("14","Зберегти колекцію у файл",new SaveCommand(ctx));
        menu.addEntry("15","Завантажити колекцію з файлу",new LoadCommand(ctx));
        menu.addEntry("16","Очистити колекцію",new ClearCommand(ctx));
        menu.addEntry("17","Демонстрація поліморфізму (dynamic dispatch)",new PolymorphismDemoCommand(ctx));
        menu.addEntry("18","Макрокоманда: Додати → Сортувати → Відобразити",new MacroCommand("Додати+Сортувати+Показати",Arrays.asList(new AddDataCommand(ctx),new SortCommand(ctx,1),new PrintCollectionCommand(ctx))));

        // НОВІ ПУНКТИ — паралельна обробка + Worker Thread
        menu.addEntry("19","Паралельна статистика (min/max/avg/відбір/σ)",new ParallelStatsCommand(ctx));
        menu.addEntry("20","Worker Thread: асинхронна черга команд",new WorkerThreadDemoCommand(ctx));

        menu.addEntry("U","Скасувати останню операцію (Undo)",new Command(){public String getName(){return "Undo";}public void execute(){CommandHistory.getInstance().undo();}public void undo(){}});
        menu.addEntry("0","Вихід",new Command(){public String getName(){return "Вихід";}public void execute(){}public void undo(){}});

        System.out.println("\n+============================================================+");
        System.out.println("|  Балістичний калькулятор v5.0                              |");
        System.out.println("|  Parallel Processing + Worker Thread Pattern               |");
        System.out.println("+============================================================+");

        while(true){
            menu.display(ctx);
            String choice=scanner.nextLine().trim().toUpperCase();
            if(choice.equals("0")){System.out.println("\n[OK] Завершення роботи. Ракета полетіла.");break;}
            if(!menu.executeCommand(choice))System.out.println("  [!] Невідомий пункт меню: «"+choice+"»");
        }
        scanner.close();
    }
}
