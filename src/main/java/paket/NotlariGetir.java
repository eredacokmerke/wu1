package paket;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import org.primefaces.context.RequestContext;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.TreeNode;

@ManagedBean
@SessionScoped
public class NotlariGetir implements Serializable
{
    private final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private final String DB_URL = "jdbc:mysql://"+System.getenv("OPENSHIFT_MYSQL_DB_HOST")+":"+System.getenv("OPENSHIFT_MYSQL_DB_PORT")+"/wu1?characterEncoding=utf8";
    private final String USER = System.getenv("OPENSHIFT_MYSQL_DB_USERNAME");
    private final String PASS = System.getenv("OPENSHIFT_MYSQL_DB_PASSWORD");
    
    private TreeNode agacKonular;
    private TreeNode selectedNode;
    private List<Integer> listIDler;
    private List<KayitlariGetir> listBasliklar;
    private List<String> listKonularString;//not ekleme listesinde konu combobox ini doldurmak için konu 
    private List<KonulariGetir> listKonular;//konuları düzenle ekranındaki listeyi dolduruyor 
    private String secilenKonu; //not ekleme sekmesinde seçilen konu
    private String ayrintiBaslik;//ayrinti Dialog ta gösterilecek baslik
    private String ayrintiNot;//ayrinti dialog ta gösterilecek kayit
    private String yeniKonu;//konuları düzenle ekranında kaydedilecek olan konu
    private StreamedContent indirilecekKayit;//not indirmek için

    private String kaydetBaslik;
    private String kaydetNot;
    private String kaydetKonu;

    private String guncelleBaslik;
    private String guncelleNot;
    private String guncelleKonu;
    private int guncellenecekID;

    private String gosterBaslik;
    private String gosterNot;
    private String gosterKonu;

    private String kullaniciAdi;
    private String sifre;

    public NotlariGetir()
    {
        kullaniciAdi = "";//giris yapmadan id kullanarak kayıy açıldığında null pointer hatası almamak için
    }

    public String notDuzenlePenceresiniAc(int id) //seçeneklerden düzenleyi seçince buraya geliyor
    {
        try
        {
            setGuncellenecekID(id);

            Class.forName(JDBC_DRIVER);
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

            PreparedStatement pst = conn.prepareStatement("select kayit, baslik, tblkonu.konu from tblkayit, tblkonu where tblkayit.konu=tblkonu.id and tblkayit.id=?");
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next())
            {
                String not = rs.getString("kayit");
                not = not.replaceAll("&lt;", "&amp;lt;").replaceAll("&gt;", "&amp;gt;");//html tagları gözükebilsin diye

                setGuncelleBaslik(rs.getString("baslik"));
                setGuncelleNot(not);
                setGuncelleKonu(rs.getString("tblkonu.konu"));
            }
        }
        catch (SQLException e)
        {
            //System.out.println("hata 20 : " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage("msgNotDuzenle", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [1]: " + e.getMessage(), ""));
        }
        catch (ClassNotFoundException e)
        {
            //System.out.println("hata 21 : " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage("msgNotDuzenle", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [2]: " + e.getMessage(), ""));
        }
        return "notDuzenle.xhtml";
    }

    public void notGuncelle()
    {
        if (guncelleBaslik.isEmpty() || guncelleNot.isEmpty() || guncelleKonu.isEmpty())
        {
            FacesContext.getCurrentInstance().addMessage("msgNotDuzenle", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Bütün Alanları Doldurun", ""));
        }
        else
        {
            try
            {
                guncelleNot = guncelleNot.replace("\n", "<br>");

                //linklerin yeni sekmede açılabilmesi için a href etiketine target="_blank" ekliyorum
                String yeniKayit = "";
                String[] linkler = guncelleNot.split("</a>");
                for (int i = 0; i < linkler.length - 1; i++)
                {
                    int yer = linkler[i].indexOf(">", linkler[i].indexOf("<a href"));

                    String basKisim = linkler[i].substring(0, yer);
                    String sonKisim = linkler[i].substring(yer);

                    basKisim = basKisim + " target=\"_blank\"";
                    String yeni = basKisim + sonKisim + "</a>";

                    yeniKayit = yeniKayit.concat(yeni);
                }
                guncelleNot = yeniKayit + linkler[linkler.length - 1];
                ///////////////////////////////////////////////////7

                Class.forName(JDBC_DRIVER);
                Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

                PreparedStatement pst = conn.prepareStatement("update tblkayit set baslik=?, kayit=?, konu=(select id from tblkonu where konu=?) where id=?");
                pst.setString(1, guncelleBaslik);
                pst.setString(2, guncelleNot);
                pst.setString(3, guncelleKonu);
                pst.setInt(4, getGuncellenecekID());

                int vtSonuc = pst.executeUpdate();

                pst.close();
                conn.close();

                if (vtSonuc == 1)
                {
                    FacesContext.getCurrentInstance().addMessage("msgNotDuzenle", new FacesMessage(FacesMessage.SEVERITY_INFO, "Güncellendi", ""));
                    FacesContext.getCurrentInstance().getExternalContext().redirect("girisNot.xhtml");

                    kayitListesiniGetir(getGuncelleKonu());
                    setGuncelleBaslik("");
                    setGuncelleKonu("");
                    setGuncelleNot("");
                    setGuncellenecekID(-1);
                }
                else
                {
                    FacesContext.getCurrentInstance().addMessage("msgNotDuzenle", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Güncelleme Başarısız Oldu", ""));
                }
            }
            catch (SQLException se)
            {
                ////System.out.println("hata 18 : " + se.getMessage());
                FacesContext.getCurrentInstance().addMessage("msgNotDuzenle", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [3] : " + se.getMessage(), ""));
            }
            catch (ClassNotFoundException e)
            {
                ////System.out.println("hata 19 : " + e.getMessage());
                FacesContext.getCurrentInstance().addMessage("msgNotDuzenle", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [3] : " + e.getMessage(), ""));
            }
            catch (IOException ex)
            {
                //System.out.println("hata 20 : " + ex.getMessage());
                FacesContext.getCurrentInstance().addMessage("msgNotDuzenle", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [4] : " + ex.getMessage(), ""));
            }
        }
    }

    public void kaydetNot()
    {
        if (kaydetBaslik.isEmpty() || kaydetNot.isEmpty() || kaydetKonu.isEmpty())
        {
            FacesContext.getCurrentInstance().addMessage("msgYeniNotEkle", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Bütün Alanları Doldurun", ""));
        }
        else
        {
            try
            {
                //System.out.println("kaydet not 1 : " + kaydetNot);
                kaydetNot = kaydetNot.replace("\n", "<br>");

                /*
                 System.out.println("kayit 1 : "+kayit);
                 //linklerin yeni sekmede açılabilmesi için a href etiketine target="_blank" ekliyorum
                 String yeniKayit = "";
                 String[] linkler= kayit.split("</a>");
                 for(int i=0; i<linkler.length-1; i++)//sonuncu elemanda link yok
                 {
                 int yer=linkler[i].indexOf(">", linkler[i].indexOf("<a href"));
                    
                 String basKisim=linkler[i].substring(0, yer);
                 String sonKisim=linkler[i].substring(yer);
                    
                 basKisim=basKisim+" target=\"_blank\"";
                 String yeni=basKisim+sonKisim;

                 yeniKayit=yeniKayit.concat(yeni);
                 }
                 kayit=yeniKayit;
                 System.out.println("kayit 2 : "+kayit);
                 ///////////////////////////////////////////////////7
                 */
                //linklerin yeni sekmede açılabilmesi için a href etiketine target="_blank" ekliyorum
                String yeniKayit = "";
                String[] linkler = kaydetNot.split("</a>");
                for (int i = 0; i < linkler.length - 1; i++)
                {
                    int yer = linkler[i].indexOf(">", linkler[i].indexOf("<a href"));

                    String basKisim = linkler[i].substring(0, yer);
                    String sonKisim = linkler[i].substring(yer);

                    basKisim = basKisim + " target=\"_blank\"";
                    String yeni = basKisim + sonKisim + "</a>";

                    yeniKayit = yeniKayit.concat(yeni);
                }
                kaydetNot = yeniKayit + linkler[linkler.length - 1];
                ///////////////////////////////////////////////////7

                Class.forName(JDBC_DRIVER);
                Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                PreparedStatement pst = conn.prepareStatement("insert into tblkayit(baslik, kayit, konu, tarih, sahip) values(?, ?, (select id from tblkonu where konu=?), ?, (select id from tblkullanici where kullanici_adi=?))");
                pst.setString(1, kaydetBaslik);
                pst.setString(2, kaydetNot);
                pst.setString(3, kaydetKonu);
                pst.setLong(4, new Date().getTime());
                pst.setString(5, kullaniciAdi);
                int vtSonuc = pst.executeUpdate();
                pst.close();
                conn.close();

                if (vtSonuc == 1)
                {
                    FacesContext.getCurrentInstance().addMessage("msgYeniNotEkle", new FacesMessage(FacesMessage.SEVERITY_INFO, "Kaydedildi", ""));
                    setKaydetBaslik("");
                    setKaydetNot("");
                    setKaydetKonu("");
                }
                else
                {
                    FacesContext.getCurrentInstance().addMessage("msgYeniNotEkle", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Kayıt Başarısız Oldu", ""));
                }
            }
            catch (SQLException se)
            {
                //System.out.println("hata 18 : " + se.getMessage());
                FacesContext.getCurrentInstance().addMessage("msgYeniNotEkle", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [5] : " + se.getMessage(), ""));
            }
            catch (ClassNotFoundException e)
            {
                //System.out.println("hata 19 : " + e.getMessage());
                FacesContext.getCurrentInstance().addMessage("msgYeniNotEkle", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [6] : " + e.getMessage(), ""));
            }
        }
    }

    public void kayitListesiniGetir(String agactanSecilenKonu)//agac tan seçilen konuya göre kayit listesini günceller
    {
        try
        {
            Class.forName(JDBC_DRIVER);
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
            listBasliklar = new ArrayList<KayitlariGetir>();
            int sira = 0;//not başlıkları listelenirken yanlarında sıra numarası çıksın

            if (agactanSecilenKonu.equals("hepsi"))
            {
                PreparedStatement pst = conn.prepareStatement("select baslik, id from tblkayit where sahip = (select id from tblkullanici where kullanici_adi = ?)");
                pst.setString(1, kullaniciAdi);
                ResultSet rs = pst.executeQuery();
                while (rs.next())
                {
                    String baslik = rs.getString("baslik");
                    int id = rs.getInt("id");

                    sira++;
                    listBasliklar.add(new KayitlariGetir(baslik, id, sira));
                }
            }
            else
            {
                //listBasliklar = new ArrayList<KayitlariGetir>();
                //try
                //{
                //Class.forName(JDBC_DRIVER);
                //Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

                PreparedStatement pst = conn.prepareStatement("select baslik, id from tblkayit where konu = (select id from tblkonu where konu = ?) and sahip = (select id from tblkullanici where kullanici_adi = ?)");
                pst.setString(1, agactanSecilenKonu);
                pst.setString(2, kullaniciAdi);
                ResultSet rs = pst.executeQuery();
                while (rs.next())
                {
                    String baslik = rs.getString("baslik");
                    int id = rs.getInt("id");

                    sira++;
                    listBasliklar.add(new KayitlariGetir(baslik, id, sira));
                }
                    //System.out.println("size : " + listBasliklar.size());
                //}
                    /*
                 catch (SQLException e)
                 {
                 //System.out.println("hata 14 : " + e.getMessage());
                 FacesContext.getCurrentInstance().addMessage("msgTabNotlariGor", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [14]: " + e.getMessage(), ""));
                 }
                
                 catch (ClassNotFoundException e)
                 {
                 //System.out.println("hata 15 : " + e.getMessage());
                 FacesContext.getCurrentInstance().addMessage("msgTabNotlariGor", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [15]: " + e.getMessage(), ""));
                 }
                 */

                //RequestContext.getCurrentInstance().execute("PF('bui').hide()");
                //RequestContext context = RequestContext.getCurrentInstance(); 
                //context.update(":tabNot:formBasliklar");
            }
        }
        catch (SQLException e)
        {
            //System.out.println("hata 14 : " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage("msgTabNotlariGor", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [7]: " + e.getMessage(), ""));
        }
        catch (ClassNotFoundException e)
        {
            //System.out.println("hata 15 : " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage("msgTabNotlariGor", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [8]: " + e.getMessage(), ""));
        }
    }

    public void kayitListesiniGuncelle()//bir kaydi sildikten sonra kayit listesini guncelliyor
    {
        //String agactanSecilenKonu = selectedNode.toString();
        kayitListesiniGetir(selectedNode.toString());

        /*
         listBasliklar = new ArrayList<KayitlariGetir>();
         try
         {
         Class.forName(JDBC_DRIVER);
         Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

         PreparedStatement pst = conn.prepareStatement("select baslik, id from tblkayit where konu=(select id from tblKonu where konu=?)");
         pst.setString(1, agactanSecilenKonu);
         ResultSet rs = pst.executeQuery();
         while (rs.next())
         {
         String baslik = rs.getString("baslik");
         int id = rs.getInt("id");

         listBasliklar.add(new KayitlariGetir(baslik, id));
         }
         System.out.println("size : " + listBasliklar.size());
         }
         catch (SQLException e)
         {
         //System.out.println("hata 7 : " + e.getMessage());
         FacesContext.getCurrentInstance().addMessage("msgTabNotlariGor", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [7]: " + e.getMessage(), ""));

         }
         catch (ClassNotFoundException e)
         {
         //System.out.println("hata 8 : " + e.getMessage());
         FacesContext.getCurrentInstance().addMessage("msgTabNotlariGor", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [8]: " + e.getMessage(), ""));
         }
        
         //RequestContext.getCurrentInstance().execute("PF('bui').hide()");
         //RequestContext context = RequestContext.getCurrentInstance(); 
         //context.update(":tabNot:formBasliklar");
         */
    }

    public String notSayfasinaGit()
    {
        return "girisNot.xhtml?faces-redirect=true";
    }

    public void kaydiSil(int id)
    {
        try
        {
            Class.forName(JDBC_DRIVER);
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

            PreparedStatement pst = conn.prepareStatement("delete from tblkayit where id=?");
            pst.setInt(1, id);
            int silmeSonucu = pst.executeUpdate();
            //System.out.println("silme sonucu: " + silmeSonucu);
            if (silmeSonucu != 1)
            {
                //System.out.println("hata 11");
                FacesContext.getCurrentInstance().addMessage("msgTabNotlariGor", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [9]: Kayıt silinemedi", ""));
            }
        }
        catch (SQLException e)
        {
            //System.out.println("hata 1 : " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage("msgTabNotlariGor", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [10]: " + e.getMessage(), ""));
        }
        catch (ClassNotFoundException e)
        {
            //System.out.println("hata 2 : " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage("msgTabNotlariGor", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [11]: " + e.getMessage(), ""));
        }
    }

    public void notAyrintilariniGetir(int id)
    {
        try
        {
            Class.forName(JDBC_DRIVER);
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

            PreparedStatement pst = conn.prepareStatement("select baslik, tarih from tblkayit where id=?");
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next())
            {
                String strKayit = rs.getString("tarih");
                long unixSeconds = Long.parseLong(strKayit);

                Date date = new Date(unixSeconds);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // the format of your date
                String formattedDate = sdf.format(date);
                //strKayit = "<div align=\"left\">" + strKayit + "</div>";//notun sola dayalı çıkması için bütün notu dic etiketi içine alıyorum

                setAyrintiNot(formattedDate);
                setAyrintiBaslik(rs.getString("baslik"));
            }

            Map<String, Object> options = new HashMap<String, Object>();
            options.put("modal", true);
            options.put("draggable", false);
            options.put("resizable", false);
            options.put("contentHeight", 320);
            //hint: available options are modal, draggable, resizable, width, height, contentWidth and contentHeight  
            RequestContext.getCurrentInstance().openDialog("ayrinti", options, null);
        }
        catch (SQLException e)
        {
            //System.out.println("hata 22 : " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage("msgTabNotlariGor", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [12]: " + e.getMessage(), ""));
        }
        catch (ClassNotFoundException e)
        {
            //System.out.println("hata 23 : " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage("msgTabNotlariGor", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [13]: " + e.getMessage(), ""));
        }
        catch (NumberFormatException e)
        {
            //System.out.println("hata 24 : " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage("msgTabNotlariGor", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [14]: " + e.getMessage(), ""));
        }
    }

    public void kaydiGetirID()
    {
        if (!kullaniciAdi.equals(""))
        {
            String ids = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("id");
            ids = ids.replace("?", "");
            Integer idd = Integer.valueOf(ids);

            kaydiGetir(idd, kullaniciAdi, sifre);
        }
        else
        {
            try
            {
                FacesContext.getCurrentInstance().getExternalContext().redirect("giris.xhtml");
            }
            catch (IOException e)
            {
                //System.out.println("hata : " + e.getMessage());
            }
        }
    }

    public String kaydiGetir(int id, String kullaniciAdi, String sifre)
    {
        try
        {
            Class.forName(JDBC_DRIVER);
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

            PreparedStatement pst = conn.prepareStatement("select kayit, baslik from tblkayit where id=? and sahip=(select id from tblkullanici where kullanici_adi=? and sifre=?)");
            pst.setInt(1, id);
            pst.setString(2, kullaniciAdi);
            pst.setString(3, sifre);
            ResultSet rs = pst.executeQuery();
            if (rs.isBeforeFirst())
            {
                if (rs.next())
                {
                    String strKayit = rs.getString("kayit");
                    strKayit = "<div align=\"left\">" + strKayit + "</div>";//notun sola dayalı çıkması için bütün notu dic etiketi içine alıyorum

                    setGosterNot(strKayit);
                    setGosterBaslik(rs.getString("baslik"));
                    //setGosterKonu("konu");
                }
                return "notGoster.xhtml?id=" + String.valueOf(id) + "?faces-redirect=true";
            }
            else//girilen id de kayıt yoksa girisNot sayfasına yönleniyor
            {
                try
                {
                    FacesContext.getCurrentInstance().getExternalContext().redirect("girisNot.xhtml");
                }
                catch (IOException e)
                {
                    //System.out.println("hata : " + e.getMessage());
                }
                return null;
            }
        }
        catch (SQLException e)
        {
            //System.out.println("hata 3 : " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage("msgTabNotlariGor", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [15]: " + e.getMessage(), ""));
            return null;
        }
        catch (ClassNotFoundException e)
        {
            //System.out.println("hata 4 : " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage("msgTabNotlariGor", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [16]: " + e.getMessage(), ""));
            return null;
        }
    }

    public String kaydiGetir(int id)
    {
        try
        {
            Class.forName(JDBC_DRIVER);
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

            PreparedStatement pst = conn.prepareStatement("select kayit, baslik from tblkayit where id=?");
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next())
            {
                String strKayit = rs.getString("kayit");
                strKayit = "<div align=\"left\">" + strKayit + "</div>";//notun sola dayalı çıkması için bütün notu dic etiketi içine alıyorum

                setGosterNot(strKayit);
                setGosterBaslik(rs.getString("baslik"));
                //setGosterKonu("konu");
            }

            return "notGoster.xhtml?id=" + String.valueOf(id) + "?faces-redirect=true";
        }
        catch (SQLException e)
        {
            //System.out.println("hata 3 : " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage("msgTabNotlariGor", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [17]: " + e.getMessage(), ""));
            return null;
        }
        catch (ClassNotFoundException e)
        {
            //System.out.println("hata 4 : " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage("msgTabNotlariGor", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [18]: " + e.getMessage(), ""));
            return null;
        }
    }

    public final void agaciOlustur()
    {
        //agacKonular = new DefaultTreeNode("K1", null);
        konulariGetir();
    }

    public void konulariGetir()
    {
        agacKonular = new DefaultTreeNode("K1", null);
        List<KonulariGetir> LKonular = new ArrayList<KonulariGetir>();
        List<String> LKonularString = new ArrayList<String>();        
        try
        {
            Class.forName(JDBC_DRIVER);
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

            PreparedStatement pst = conn.prepareStatement("select konu, id from tblkonu where sahip = (select id from tblkullanici where kullanici_adi = ?)");
            pst.setString(1, kullaniciAdi);
            ResultSet rs = pst.executeQuery();
            while (rs.next())
            {
                String konu = rs.getString("konu");
                int id = rs.getInt("id");

                LKonular.add(new KonulariGetir(konu, id));
                LKonularString.add(konu);
                new DefaultTreeNode(konu, agacKonular); //konuyu agaca ekliyor
            }
            new DefaultTreeNode("hepsi", agacKonular);//bütün notları gösterecek konu
            
            setListKonular(LKonular);
            setListKonularString(LKonularString);
        }
        catch (SQLException e)
        {
            //System.out.println("hata 5 : " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage("msgTabNotlariGor", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [19]: " + e.getMessage(), ""));
        }
        catch (ClassNotFoundException e)
        {
            //System.out.println("hata 6 : " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage("msgTabNotlariGor", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [20]: " + e.getMessage(), ""));
        }
    }

    public void onNodeSelect(NodeSelectEvent event)//agactan konu secince kayıtları getiriyor
    {
        kayitListesiniGetir(event.getTreeNode().toString());

        /*
         listBasliklar = new ArrayList<KayitlariGetir>();
         try
         {
         Class.forName(JDBC_DRIVER);
         Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

         PreparedStatement pst = conn.prepareStatement("select baslik, id from tblkayit where konu=(select id from tblKonu where konu=?)");
         pst.setString(1, agactanSecilenKonu);
         ResultSet rs = pst.executeQuery();
         while (rs.next())
         {
         String baslik = rs.getString("baslik");
         int id = rs.getInt("id");
                
         System.out.println("baslik: "+baslik);

         listBasliklar.add(new KayitlariGetir(baslik, id));
         }
         System.out.println("size : " + listBasliklar.size());
         }
         catch (SQLException e)
         {
         //System.out.println("hata 7 : " + e.getMessage());
         FacesContext.getCurrentInstance().addMessage("msgTabNotlariGor", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [7]: " + e.getMessage(), ""));

         }
         catch (ClassNotFoundException e)
         {
         //System.out.println("hata 8 : " + e.getMessage());
         FacesContext.getCurrentInstance().addMessage("msgTabNotlariGor", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [8]: " + e.getMessage(), ""));
         }   
         */
    }

    public void konuSil(int id)//konu düzenle ekranında konuyu siliyor
    {
        try
        {
            Class.forName(JDBC_DRIVER);
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
            PreparedStatement pst = conn.prepareStatement("select count(*) from tblkayit where konu=?");
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            while (rs.next())
            {
                int count = rs.getInt(1);
                if (count != 0)
                {
                    FacesContext.getCurrentInstance().addMessage("msgKonulariDuzenle", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Konuya Ait Kayıtlar Var", ""));
                }
                else
                {
                    PreparedStatement pst2 = conn.prepareStatement("delete from tblkonu where id=?");
                    pst2.setInt(1, id);
                    int silmeSonucu = pst2.executeUpdate();
                    if (silmeSonucu == 1)
                    {
                        konulariGetir();
                        FacesContext.getCurrentInstance().addMessage("msgKonulariDuzenle", new FacesMessage(FacesMessage.SEVERITY_INFO, "Konu Silindi", ""));
                    }
                    else
                    {
                        FacesContext.getCurrentInstance().addMessage("msgKonulariDuzenle", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Konu Silinirken Hata Oluştu [21]", ""));
                    }
                }
            }
        }
        catch (ClassNotFoundException ex)
        {
            //System.out.println("hata 12 : " + ex.getMessage());
            FacesContext.getCurrentInstance().addMessage("msgKonulariDuzenle", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [22]: " + ex.getMessage(), ""));
        }
        catch (SQLException ex)
        {
            //System.out.println("hata 13 : " + ex.getMessage());
            FacesContext.getCurrentInstance().addMessage("msgKonulariDuzenle", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [23]: " + ex.getMessage(), ""));
        }
    }

    public void konuKaydet()//yeni konuyu vt ye kaydediyor
    {
        if (yeniKonu.equals(""))
        {
            FacesContext.getCurrentInstance().addMessage("msgKonulariDuzenle", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Konu İsmi Boş Olamaz", ""));
        }
        else
        {
            try
            {
                Class.forName(JDBC_DRIVER);
                Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

                PreparedStatement pst = conn.prepareStatement("select count(*) from tblkonu where upper (konu) = upper(?) and sahip = (select id from tblkullanici where kullanici_adi = ?)");
                pst.setString(1, yeniKonu);
                pst.setString(2, kullaniciAdi);
                ResultSet rs = pst.executeQuery();
                while (rs.next())
                {
                    int count = rs.getInt(1);
                    if (count != 0)
                    {
                        FacesContext.getCurrentInstance().addMessage("msgKonulariDuzenle", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Konu Zaten Var", ""));
                    }
                    else
                    {
                        PreparedStatement pst2 = conn.prepareStatement("insert into tblKonu(konu, sahip) values(?, (select id from tblkullanici where kullanici_adi = ?))");
                        pst2.setString(1, yeniKonu);
                        pst2.setString(2, kullaniciAdi);
                        int vtSonuc = pst2.executeUpdate();
                        //System.out.println("vtSonuc: " + vtSonuc);
                        if (vtSonuc == 1)
                        {
                            yeniKonu = "";
                            konulariGetir();
                            FacesContext.getCurrentInstance().addMessage("msgKonulariDuzenle", new FacesMessage(FacesMessage.SEVERITY_INFO, "Kaydedildi", ""));
                        }
                        else
                        {
                            FacesContext.getCurrentInstance().addMessage("msgKonulariDuzenle", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Kaydedilemedi", ""));
                        }
                    }
                }
            }
            catch (SQLException e)
            {
                //System.out.println("hata 9 : " + e.getMessage());
                FacesContext.getCurrentInstance().addMessage("msgKonulariDuzenle", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [24]: " + e.getMessage(), ""));
            }
            catch (ClassNotFoundException e)
            {
                //System.out.println("hata 10: " + e.getMessage());
                FacesContext.getCurrentInstance().addMessage("msgKonulariDuzenle", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [25]: " + e.getMessage(), ""));
            }
        }

        //System.out.println("listkonular: " + listKonularString);
        //UIComponent comp=FacesContext.getCurrentInstance().getViewRoot().findComponent("menuKonu");
        /*
         SelectOneMenu som= new SelectOneMenu();
        
         UIComponent comp=FacesContext.getCurrentInstance().getViewRoot().findComponent("menuKonu");
         som=(SelectOneMenu) comp;
         System.out.println("som.getColums(): "+som.getChildCount());
        
         RequestContext.getCurrentInstance().update("formYeniNotEkle:menuKonu");
         */
        //RequestContext.getCurrentInstance().update(som.toString());
        //som.asetValue("sqwewqewqeqwe");
        //System.out.println("som.getColums(): "+som.setValue(new String("qq")));
        //FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add(":tabNot:formYeniNotEkle:menuKonu");
        //RequestContext.getCurrentInstance().update(":tabNot:formYeniNotEkle:menuKonu");
    }

    public void dialogKonulariDuzenleAc()//konuları düzenle diyalogunu acar
    {
        Map<String, Object> options = new HashMap<String, Object>();
        options.put("modal", true);
        options.put("draggable", false);
        options.put("resizable", false);
        RequestContext.getCurrentInstance().openDialog("konulariDuzenle", options, null);
    }

    public String kullaniciKaydiYap()
    {
        if (kullaniciAdi.isEmpty() || sifre.isEmpty())
        {
            FacesContext.getCurrentInstance().addMessage("msgKayitOl", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Boş Alan Var", ""));
            return null;
        }
        else
        {
            try
            {
                Class.forName(JDBC_DRIVER);
                Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

                PreparedStatement pst = conn.prepareStatement("insert into tblKullanici (kullanici_adi, sifre) values(?, ?)");
                pst.setString(1, kullaniciAdi);
                pst.setString(2, sifre);
                int sonuc = pst.executeUpdate();

                if (sonuc == 1)
                {
                    //setSifre("");
                    //setKullaniciAdi("");
                    return "secenek.xhtml?faces-redirect=true";
                }
                else
                {
                    //System.out.println("kayit hata");
                    FacesContext.getCurrentInstance().addMessage("msgKayitOl", new FacesMessage(FacesMessage.SEVERITY_ERROR, "kayıt hata", ""));
                    //setSifre("");
                    //setKullaniciAdi("");
                    return null;
                }
            }
            catch (SQLException e)
            {
                //System.out.println("hata 9 : " + e.getMessage());
                FacesContext.getCurrentInstance().addMessage("msgKayitOl", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [26]: " + e.getMessage(), ""));
                //setSifre("");
                //setKullaniciAdi("");
                return null;
            }
            catch (ClassNotFoundException e)
            {
                //System.out.println("hata 10: " + e.getMessage());
                FacesContext.getCurrentInstance().addMessage("msgKayitOl", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [27]: " + e.getMessage(), ""));
                //setSifre("");
                //setKullaniciAdi("");
                return null;
            }
        }
    }

    public String kayitOl()
    {
        return "kayitOl.xhtml?faces-redirect=true";
    }

    public String kullaniciGirisiYap()
    {        
        if (kullaniciAdi.isEmpty() || sifre.isEmpty())
        {
            FacesContext.getCurrentInstance().addMessage("msgGiris", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Boş Alan Var", ""));
            return null;
        }
        else
        {
            try
            {
                Class.forName(JDBC_DRIVER);
                Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

                PreparedStatement pst = conn.prepareStatement("select count(*) from tblkullanici where sifre=? and kullanici_adi=?");
                pst.setString(1, sifre);
                pst.setString(2, kullaniciAdi);
                ResultSet rs = pst.executeQuery();
                while (rs.next())
                {
                    if (rs.getInt(1) == 1)
                    {
                        //setSifre("");
                        //setKullaniciAdi("");
                        //return "secenek.xhtml?faces-redirect=true";
                        return "girisNot.xhtml?faces-redirect=true";
                    }
                    else
                    {
                        System.out.println("giris hata");
                        FacesContext.getCurrentInstance().addMessage("msgGiris", new FacesMessage(FacesMessage.SEVERITY_ERROR, "kullanıcı yok", ""));
                        //setSifre("");
                        //setKullaniciAdi("");
                        return null;
                    }
                }
                return null;
            }
            catch (SQLException e)
            {
                //System.out.println("hata 9 : " + e.getMessage());
                FacesContext.getCurrentInstance().addMessage("msgGiris", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [28]: " + e.getMessage(), ""));
                //setSifre("");
                //setKullaniciAdi("");
                return null;
            }
            catch (ClassNotFoundException e)
            {
                //System.out.println("hata 10: " + e.getMessage());
                FacesContext.getCurrentInstance().addMessage("msgGiris", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata Oluştu [29]: " + e.getMessage(), ""));
                //setSifre("");
                //setKullaniciAdi("");
                return null;
            }
        }
    }
    
    public class KayitlariGetir implements Serializable//agactan secilen konunun kayitlari.
    {

        private String baslik;
        private int id;
        private int sira;

        public KayitlariGetir(String baslik, int id, int sira)
        {
            this.baslik = baslik;
            this.id = id;
            this.sira = sira;
        }

        public String getBaslik()
        {
            return baslik;
        }

        public void setBaslik(String baslik)
        {
            this.baslik = baslik;
        }

        public int getId()
        {
            return id;
        }

        public void setId(int id)
        {
            this.id = id;
        }

        public int getSira()
        {
            return sira;
        }

        public void setSira(int sira)
        {
            this.sira = sira;
        }
    }

    public static class KonulariGetir implements Serializable//konuları düzenle ekranı  için
    {

        private String konu;
        private int id;

        public KonulariGetir(String konu, int id)
        {
            this.konu = konu;
            this.id = id;
        }

        public KonulariGetir()
        {
        }

        public String getKonu()
        {
            return konu;
        }

        public void setKonu(String konu)
        {
            this.konu = konu;
        }

        public int getId()
        {
            return id;
        }

        public void setId(int id)
        {
            this.id = id;
        }
    }

    public List<Integer> getListIDler()
    {
        return listIDler;
    }

    public void setListIDler(List<Integer> listIDler)
    {
        this.listIDler = listIDler;
    }

    public TreeNode getSelectedNode()
    {
        return selectedNode;
    }

    public void setSelectedNode(TreeNode selectedNode)
    {
        this.selectedNode = selectedNode;
    }

    public TreeNode getAgacKonular()
    {
        return agacKonular;
    }

    public void setAgacKonular(TreeNode agacKonular)
    {
        this.agacKonular = agacKonular;
    }

    public List<KonulariGetir> getListKonular()
    {
        return listKonular;
    }

    public void setListKonular(List<KonulariGetir> listKonular)
    {
        this.listKonular = listKonular;
    }

    public List<KayitlariGetir> getListBasliklar()
    {
        return listBasliklar;
    }

    public void setListBasliklar(List<KayitlariGetir> listBasliklar)
    {
        this.listBasliklar = listBasliklar;
    }

    public String getSecilenKonu()
    {
        return secilenKonu;
    }

    public void setSecilenKonu(String secilenKonu)
    {
        this.secilenKonu = secilenKonu;
    }

    public String getAyrintiBaslik()
    {
        return ayrintiBaslik;
    }

    public void setAyrintiBaslik(String ayrintiBaslik)
    {
        this.ayrintiBaslik = ayrintiBaslik;
    }

    public String getAyrintiNot()
    {
        return ayrintiNot;
    }

    public void setAyrintiNot(String ayrintiNot)
    {
        this.ayrintiNot = ayrintiNot;
    }

    public String getYeniKonu()
    {
        return yeniKonu;
    }

    public void setYeniKonu(String yeniKonu)
    {
        this.yeniKonu = yeniKonu;
    }

    public List<String> getListKonularString()
    {
        return listKonularString;
    }

    public void setListKonularString(List<String> listKonularString)
    {
        this.listKonularString = listKonularString;
    }

    public StreamedContent getIndirilecekKayit()
    {
        return indirilecekKayit;
    }

    public void setIndirilecekKayit(StreamedContent indirilecekKayit)
    {
        this.indirilecekKayit = indirilecekKayit;
    }

    public String getKaydetBaslik()
    {
        return kaydetBaslik;
    }

    public void setKaydetBaslik(String kaydetBaslik)
    {
        this.kaydetBaslik = kaydetBaslik;
    }

    public String getKaydetNot()
    {
        return kaydetNot;
    }

    public void setKaydetNot(String kaydetNot)
    {
        this.kaydetNot = kaydetNot;
    }

    public String getKaydetKonu()
    {
        return kaydetKonu;
    }

    public void setKaydetKonu(String kaydetKonu)
    {
        this.kaydetKonu = kaydetKonu;
    }

    public String getGuncelleBaslik()
    {
        return guncelleBaslik;
    }

    public void setGuncelleBaslik(String guncelleBaslik)
    {
        this.guncelleBaslik = guncelleBaslik;
    }

    public String getGuncelleNot()
    {
        return guncelleNot;
    }

    public void setGuncelleNot(String guncelleNot)
    {
        this.guncelleNot = guncelleNot;
    }

    public String getGuncelleKonu()
    {
        return guncelleKonu;
    }

    public void setGuncelleKonu(String guncelleKonu)
    {
        this.guncelleKonu = guncelleKonu;
    }

    public int getGuncellenecekID()
    {
        return guncellenecekID;
    }

    public void setGuncellenecekID(int guncellenecekID)
    {
        this.guncellenecekID = guncellenecekID;
    }

    public String getGosterBaslik()
    {
        return gosterBaslik;
    }

    public void setGosterBaslik(String gosterBaslik)
    {
        this.gosterBaslik = gosterBaslik;
    }

    public String getGosterNot()
    {
        return gosterNot;
    }

    public void setGosterNot(String gosterNot)
    {
        this.gosterNot = gosterNot;
    }

    public String getGosterKonu()
    {
        return gosterKonu;
    }

    public void setGosterKonu(String gosterKonu)
    {
        this.gosterKonu = gosterKonu;
    }

    public String getKullaniciAdi()
    {
        return kullaniciAdi;
    }

    public void setKullaniciAdi(String kullaniciAdi)
    {
        this.kullaniciAdi = kullaniciAdi;
    }

    public String getSifre()
    {
        return sifre;
    }

    public void setSifre(String sifre)
    {
        this.sifre = sifre;
    }
}
