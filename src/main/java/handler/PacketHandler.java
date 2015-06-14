package handler;

import java.net.InetAddress;
import java.net.UnknownHostException;

import ndpi.dpiadapt;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.packet.IPv4;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.PacketException;
import org.opendaylight.controller.sal.packet.PacketResult;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class PacketHandler implements IListenDataPacket {

    private static final Logger log = LoggerFactory.getLogger(PacketHandler.class);
    private IDataPacketService dataPacketService;

    static private InetAddress intToInetAddress(int i) {
        byte b[] = new byte[] { (byte) ((i>>24)&0xff), (byte) ((i>>16)&0xff), (byte) ((i>>8)&0xff), (byte) (i&0xff) };
        InetAddress addr;
        try {
            addr = InetAddress.getByAddress(b);
        } catch (UnknownHostException e) {
            return null;
        }

        return addr;
    }

    /*
     * Sets a reference to the requested DataPacketService
     * See Activator.configureInstance(...):
     * c.add(createContainerServiceDependency(containerName).setService(
     * IDataPacketService.class).setCallbacks(
     * "setDataPacketService", "unsetDataPacketService")
     * .setRequired(true));
     */
    void setDataPacketService(IDataPacketService s) {
        log.trace("Set DataPacketService.");

        dataPacketService = s;
    }

    /*
     * Unsets DataPacketService
     * See Activator.configureInstance(...):
     * c.add(createContainerServiceDependency(containerName).setService(
     * IDataPacketService.class).setCallbacks(
     * "setDataPacketService", "unsetDataPacketService")
     * .setRequired(true));
     */
    void unsetDataPacketService(IDataPacketService s) {
        log.trace("Removed DataPacketService.");

        if (dataPacketService == s) {
            dataPacketService = null;
        }
    }


    public PacketResult receiveDataPacket(RawPacket inPkt) {
        log.trace("Received data packet.");

        // The connector, the packet came from ("port")
        NodeConnector ingressConnector = inPkt.getIncomingNodeConnector();
        // The node that received the packet ("switch")
        Node node = ingressConnector.getNode();
        // Use DataPacketService to decode the packet.
        Packet l2pkt = dataPacketService.decodeDataPacket(inPkt);

        int ipoffset;

        if (l2pkt instanceof Ethernet) {
        	if (l2pkt.getfieldOffset("EtherType") == 8100) ipoffset = 32;
        	else ipoffset = 28;

        	//System.out.println(l2pkt.toString());
        	Packet l3Pkt = l2pkt.getPayload();

            if (l3Pkt instanceof IPv4) {
                IPv4 ipv4Pkt = (IPv4) l3Pkt;

                int sizeHeader = ipv4Pkt.getHeaderSize();
                int ipSize = ipv4Pkt.getTotalLength() - ipoffset;
                byte[] header = new byte[sizeHeader];

                try {
					byte[] paquete = ipv4Pkt.serialize();

					for (int i=0; i<sizeHeader; ++i)
					{
						header[i] = paquete[i];
						//System.out.println(i + "\n");
					}

				} catch (PacketException e) {

					e.printStackTrace();
				}

                dpiadapt h;
                h =  new dpiadapt();

                int res = h.sendPacket(header, ipoffset, ipSize, sizeHeader);
                	/*	System.out.println("getHeaderSize =  " + sizeHeader + "\n" +
                		"ipoffset =  " + ipoffset + "\n" +
                		"getHeaderLen =  " + ipSize + "\n");*/



               // dpiadapt h = new dpiadapt () ;
                System.out.println("trolololoooo");
           //     int res =  h.sendPacket(header, ipoffset, ipSize, sizeHeader);
                System.out.println("tralalalaaaaa");
          //     System.out.println(res + "\n");

                int dstAddr = ipv4Pkt.getDestinationAddress();
                InetAddress addr = intToInetAddress(dstAddr);

                return PacketResult.KEEP_PROCESSING;
            }
        }
        // We did not process the packet -> let someone else do the job.
        return PacketResult.IGNORED;
    }
}
