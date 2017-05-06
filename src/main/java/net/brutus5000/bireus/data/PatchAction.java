package net.brutus5000.bireus.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PatchAction {
    ADD("add"),
    REMOVE("remove"),
    DELTA("delta"),
    BSDIFF("bsdiff"),
    ZIPDELTA("zipdelta");

    String value;
}
