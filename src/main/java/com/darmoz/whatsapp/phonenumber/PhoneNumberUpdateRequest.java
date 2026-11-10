package com.darmoz.whatsapp.phonenumber;

import java.util.UUID;

/**
 * botId: el bot que atiende el numero. Para desasignarlo se manda
 * unassignBot=true (null en botId significa "no tocar").
 */
public record PhoneNumberUpdateRequest(UUID botId, Boolean unassignBot, Boolean active) {
}
