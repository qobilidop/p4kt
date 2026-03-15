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

control TopPipe(inout Ipv4_h headers_ip, inout Ethernet_h headers_ethernet, out OutControl outCtrl) {
    action Drop_action() {
        outCtrl.outputPort = DROP_PORT;
    }
    IPv4Address nextHop;
    action Set_nhop(IPv4Address ipv4_dest, PortId port) {
        nextHop = ipv4_dest;
        headers_ip.ttl = (headers_ip.ttl - 1);
        outCtrl.outputPort = port;
    }
    table ipv4_match {
        key = { headers_ip.dstAddr : lpm; }
        actions = {
            Drop_action;
            Set_nhop;
        }
        size = 1024;
        default_action = Drop_action;
    }
    action Send_to_cpu() {
        outCtrl.outputPort = CPU_OUT_PORT;
    }
    table check_ttl {
        key = { headers_ip.ttl : exact; }
        actions = {
            Send_to_cpu;
        }
        const default_action = Send_to_cpu;
    }
    action Set_dmac(EthernetAddress dmac) {
        headers_ethernet.dstAddr = dmac;
    }
    table dmac {
        key = { nextHop : exact; }
        actions = {
            Drop_action;
            Set_dmac;
        }
        size = 1024;
        default_action = Drop_action;
    }
    action Set_smac(EthernetAddress smac) {
        headers_ethernet.srcAddr = smac;
    }
    table smac {
        key = { outCtrl.outputPort : exact; }
        actions = {
            Drop_action;
            Set_smac;
        }
        size = 16;
        default_action = Drop_action;
    }
    apply {
        ipv4_match.apply();
        if ((outCtrl.outputPort == DROP_PORT)) {
            return;
        }
        check_ttl.apply();
        if ((outCtrl.outputPort == CPU_OUT_PORT)) {
            return;
        }
        dmac.apply();
        if ((outCtrl.outputPort == DROP_PORT)) {
            return;
        }
        smac.apply();
    }
}
