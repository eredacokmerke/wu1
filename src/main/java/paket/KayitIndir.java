package paket;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import org.jsoup.Jsoup;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

@ManagedBean
@SessionScoped
public class KayitIndir implements Serializable
{

    private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private final String DB_URL = "jdbc:mysql://"+System.getenv("OPENSHIFT_MYSQL_DB_HOST")+":"+System.getenv("OPENSHIFT_MYSQL_DB_PORT")+"/wu1?characterEncoding=utf8";
    private final String USER = System.getenv("OPENSHIFT_MYSQL_DB_USERNAME");
    private final String PASS = System.getenv("OPENSHIFT_MYSQL_DB_PASSWORD");
    
    private StreamedContent indirilecekKayit;

    public KayitIndir()
    {

    }

    public void indir(int id)
    {
        try
        {
            Class.forName(JDBC_DRIVER);
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
            PreparedStatement pst = conn.prepareStatement("select kayit, baslik from tblkayit where id=?");
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            while (rs.next())
            {
                String strKayit = rs.getString(1);
                String strBaslik = rs.getString(2);

                strKayit = strKayit.replaceAll("<div", "NL");
                strKayit = strKayit.replaceAll("<br>", "NL");

                String a = Jsoup.parse(strKayit).text();
                a = a.replaceAll("NL", "\n");

                //String b=StringEscapeUtils.unescapeHtml4(a);
                InputStream input = new ByteArrayInputStream(a.getBytes());
                setIndirilecekKayit(new DefaultStreamedContent(input, "text/plain", strBaslik));
            }
        }
        catch (SQLException ex)
        {
            //System.out.println("hata 16 : " + ex.getMessage());
            FacesContext.getCurrentInstance().addMessage("msgTabNotlariGor", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [30] : " + ex.getMessage(), ""));
        }
        catch (ClassNotFoundException ex)
        {
            //System.out.println("hata 17 : " + ex.getMessage());
            FacesContext.getCurrentInstance().addMessage("msgTabNotlariGor", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [31]: " + ex.getMessage(), ""));
        }

            //String str = "This is a String ~ GoGoGo";
        //InputStream input = new ByteArrayInputStream(str.getBytes());
        //setIndirilecekKayit(new DefaultStreamedContent(input, "text/plain", "isim"));
        //System.out.println("name : " + file.getName());
        //System.out.println("mime : " + externalContext.getMimeType(file.getName()));
        //System.out.println("dosya yolu : " + file.getAbsolutePath());
        /*
         try
         {
         File file = new File("/home/ekcdr/Resimler/tesla/1.png");
         System.out.println("dosya yolu : " + file.getAbsolutePath());
         InputStream input = new FileInputStream(file);
         ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
         setIndirilecekKayit(new DefaultStreamedContent(input, externalContext.getMimeType(file.getName()), "isim"));
         System.out.println("name : " + file.getName());
         System.out.println("mime : " + externalContext.getMimeType(file.getName()));
         }
         catch (FileNotFoundException ex)
         {
         System.out.println("HATA : " + ex.getLocalizedMessage());
         }
         */
        //  InputStream stream = ((ServletContext)FacesContext.getCurrentInstance().getExternalContext().getContext()).getResourceAsStream("/home/ekcdr/Resimler/tesla/1.png");  
        // indirilecekKayit = new DefaultStreamedContent(stream, "image/jpg", "resim.png");  
    }

    public StreamedContent getIndirilecekKayit()
    {
        return indirilecekKayit;
    }

    public void setIndirilecekKayit(StreamedContent indirilecekKayit)
    {
        this.indirilecekKayit = indirilecekKayit;
    } 
}
