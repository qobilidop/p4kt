header Ipv4_h {
    bit<8> ttl;
}

function void headers(inout Ipv4_h ip) {
    ip.ttl = (ip.ttl - 1);
}
