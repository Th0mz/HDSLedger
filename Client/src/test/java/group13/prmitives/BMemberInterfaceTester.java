package group13.prmitives;

import group13.blockchain.member.BMember;
import group13.blockchain.member.BMemberInterface;
import group13.primitives.Address;

import java.security.PrivateKey;
import java.security.PublicKey;

public class BMemberInterfaceTester extends BMemberInterface {
    public BMemberInterfaceTester(PublicKey publicKey, PrivateKey privateKey, Address memberAddress, BMember server) {
        super(publicKey, privateKey, memberAddress, server);
    }
}
