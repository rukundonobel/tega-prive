package com.tegaprive.payments;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlatformSettingsRepository extends JpaRepository<PlatformSetting, String> {

  @Query("SELECT s FROM PlatformSetting s WHERE s.key = :key")
  PlatformSetting findByKey(@Param("key") String key);
}
