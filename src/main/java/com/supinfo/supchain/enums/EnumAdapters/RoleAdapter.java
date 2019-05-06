package com.supinfo.supchain.enums.EnumAdapters;

import com.supinfo.supchain.enums.Role;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class RoleAdapter extends XmlAdapter<String, Role> {

    @Override
    public String marshal(Role role) throws Exception {
        return role.name();
    }

    @Override
    public Role unmarshal(String string) throws Exception {
        try {
            return Role.valueOf(string);
        } catch(Exception e) {
            throw new JAXBException(e);
        }
    }
}
