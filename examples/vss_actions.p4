typedef bit<4> PortId;

typedef bit<32> IPv4Address;

const PortId DROP_PORT = 4w15;

header Ethernet_h {
    bit<48> dstAddr;
    bit<48> srcAddr;
    bit<16> etherType;
}

header Ipv4_h {
    bit<8> ttl;
    IPv4Address srcAddr;
    IPv4Address dstAddr;
}

struct OutControl {
    PortId outputPort;
}

action Drop_action(inout OutControl outCtrl) {
    outCtrl.outputPort = DROP_PORT;
}

action Set_nhop(IPv4Address ipv4_dest, PortId port, inout Ipv4_h headers_ip, inout OutControl outCtrl) {
    nextHop = ipv4_dest;
    headers_ip.ttl = (headers_ip.ttl - 1);
    outCtrl.outputPort = port;
}
