package net.brutus5000.bireus.service;

import lombok.Getter;
import net.brutus5000.bireus.data.Repository;

@Getter
public class CheckoutException extends Exception {
    final Repository repository;
    final String targetVersion;

    public CheckoutException(String reason, Repository repository, String targetVersion, Throwable cause) {
        super(reason, cause);
        this.repository = repository;
        this.targetVersion = targetVersion;
    }

    public CheckoutException(String reason, Repository repository, String targetVersion) {
        super(reason);
        this.repository = repository;
        this.targetVersion = targetVersion;
    }
}
