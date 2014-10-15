package paket;

import java.io.Serializable;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import org.primefaces.context.RequestContext;

@ManagedBean
@SessionScoped
public class KonulariDuzenle implements Serializable
{
    public KonulariDuzenle()
    {
    }
    
    /*
    public void dialoguAc()
    {
        Map<String, Object> options = new HashMap<String, Object>();
        options.put("modal", true);
        options.put("draggable", false);
        options.put("resizable", false);
        options.put("contentHeight", 320);
        RequestContext.getCurrentInstance().openDialog("konulariDuzenle", options, null);
    }
    */
    
    public void dialoguKapat(NotlariGetir ng)
    {  
        //System.out.println("dialoguKapat");
        RequestContext.getCurrentInstance().closeDialog(ng);
    }    
}
