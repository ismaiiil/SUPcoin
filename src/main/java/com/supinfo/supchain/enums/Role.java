package com.supinfo.supchain.enums;

import com.supinfo.supchain.enums.EnumAdapters.RoleAdapter;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlJavaTypeAdapter(RoleAdapter.class)
public enum Role{
    EDGE,RDV
}
