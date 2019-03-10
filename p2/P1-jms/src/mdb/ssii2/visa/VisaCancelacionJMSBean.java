/**
 * Pr&aacute;ctricas de Sistemas Inform&aacute;ticos II
 * VisaCancelacionJMSBean.java
 */

package ssii2.visa;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.ejb.ActivationConfigProperty;
import javax.jms.MessageListener;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.JMSException;
import javax.annotation.Resource;
import java.util.logging.Logger;

/**
 * @author jaime
 */
@MessageDriven(mappedName = "jms/VisaPagosQueue")
public class VisaCancelacionJMSBean extends DBTester implements MessageListener {
  static final Logger logger = Logger.getLogger("VisaCancelacionJMSBean");
  @Resource
  private MessageDrivenContext mdc;

  private static final String UPDATE_CANCELA_QRY = 
      "update pagos set codrespuesta=? where idAutorizacion=?";

  private static final String SELECT_SALDO_TARJETA_QRY = 
      "select numeroTarjeta, importe from pago where idAutorizacion=?";

  private static final String ROLLBACK_SALDO_QRY = 
      "update tarjeta set saldo=saldo+? where numeroTarjeta=?";

  public VisaCancelacionJMSBean() {
  }

  public void onMessage(Message inMessage) {
      TextMessage msg = null;
      Connection con = null;
      ResultSet rs = null;
      String qry = null;
      PreparedStatement pstmt = null;
      boolean ret=true;
      String numero_tarjeta="";
      double importe_pago_saldo=0.0;

      try {
          if (inMessage instanceof TextMessage) {
               msg = (TextMessage) inMessage;
               logger.info("MESSAGE BEAN: Message received: " + msg.getText());
          
               con = getConnection();
           
               String cancela_qry=UPDATE_CANCELA_QRY;
               pstmt = con.prepareStatement(cancela_qry);
               pstmt.setString(1, "999");
               pstmt.setInt(2, Integer.parseInt(msg.getText()));
               ret = false;
               if (!pstmt.execute()
                      && pstmt.getUpdateCount() == 1) {
                 ret = true;
               }
               
               String select_qry=SELECT_SALDO_TARJETA_QRY;
               pstmt = con.prepareStatement(select_qry);
               pstmt.setInt(1, Integer.parseInt(msg.getText()));
               rs = pstmt.executeQuery();
               if(rs.next()){
                numero_tarjeta=rs.getString("numeroTarjeta");
                importe_pago_saldo=rs.getDouble("importe");
               }
               
               String rollback_qry=ROLLBACK_SALDO_QRY;
               pstmt = con.prepareStatement(rollback_qry);
               pstmt.setDouble(1, importe_pago_saldo);
               pstmt.setString(2, numero_tarjeta);
               ret = false;
               if (!pstmt.execute()
                      && pstmt.getUpdateCount() == 1) {
                 ret = true;
               }
          } else {
               logger.warning(
                      "Message of wrong type: "
                      + inMessage.getClass().getName());
          }
      } catch (JMSException e) {
          e.printStackTrace();
          mdc.setRollbackOnly();
      } catch (Throwable te) {
          te.printStackTrace();
      }
      try{
        if (rs != null) {
            rs.close(); rs = null;
        }
        if (pstmt != null) {
            pstmt.close(); pstmt = null;
        }
        if (con != null) {
            closeConnection(con); con = null;
        }
      }
      catch(Exception e){
      
      }
  }


}
