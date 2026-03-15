typedef bit<48> EthernetAddress;

typedef bit<32> IPv4Address;

typedef bit<4> PortId;

const PortId DROP_PORT = 4w15;

const PortId CPU_OUT_PORT = 4w14;

header Ethernet_h {
    EthernetAddress dstAddr;
    EthernetAddress srcAddr;
    bit<16> etherType;
}

header Ipv4_h {
    bit<4> version;
    bit<4> ihl;
    bit<8> diffserv;
    bit<16> totalLen;
    bit<16> identification;
    bit<3> flags;
    bit<13> fragOffset;
    bit<8> ttl;
    bit<8> protocol;
    bit<16> hdrChecksum;
    IPv4Address srcAddr;
    IPv4Address dstAddr;
}

struct Parsed_packet {
    Ethernet_h ethernet;
    Ipv4_h ip;
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

action Send_to_cpu(inout OutControl outCtrl) {
    outCtrl.outputPort = CPU_OUT_PORT;
}

action Set_dmac(EthernetAddress dmac, inout Ethernet_h headers_ethernet) {
    headers_ethernet.dstAddr = dmac;
}

action Set_smac(EthernetAddress smac, inout Ethernet_h headers_ethernet) {
    headers_ethernet.srcAddr = smac;
}
