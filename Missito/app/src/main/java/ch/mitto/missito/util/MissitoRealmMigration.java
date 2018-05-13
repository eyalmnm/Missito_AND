package ch.mitto.missito.util;

import io.realm.DynamicRealm;
import io.realm.DynamicRealmObject;
import io.realm.RealmMigration;
import io.realm.RealmObjectSchema;
import io.realm.RealmSchema;

/**
 * Created by usr1 on 12/26/17.
 */

public class MissitoRealmMigration implements RealmMigration {


    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
        RealmSchema schema = realm.getSchema();

//        if (oldVersion < 2) {
//            schema.create(RealmImage.class.getSimpleName())
//                    .addField("fileName", String.class)
//                    .addField("localFileURI", String.class)
//                    .addField("link", String.class)
//                    .addField("size", long.class)
//                    .addField("secret", String.class)
//                    .addField("thumbnail", String.class);
//
//            schema.create(RealmAttachment.class.getSimpleName())
//                    .addRealmListField("images", schema.get(RealmImage.class.getSimpleName()));
//
//
//            schema.get(RealmMessage.class.getSimpleName())
//                    .addRealmObjectField("attach", schema.get(RealmAttachment.class.getSimpleName()));
//            oldVersion++;
//        }
//
//        if (oldVersion < 3) {
//            // updating RealmMissitoContact.phone values to contain "+" as prefix
//            schema.get(RealmMissitoContact.class.getSimpleName())
//                    .removePrimaryKey()
//                    .transform(new RealmObjectSchema.Function() {
//                        @Override
//                        public void apply(DynamicRealmObject obj) {
//                            String currentPhone = obj.getString("phone");
//                            String phone = Helper.addPlus(currentPhone);
//                            obj.set("phone", phone);
//                        }
//                    })
//                    .addPrimaryKey("phone");
//
//            // updating RealmMessage.from and RealmMessage.to values to contain "+" as prefix
//            schema.get(RealmMessage.class.getSimpleName())
//                    .transform(new RealmObjectSchema.Function() {
//                        @Override
//                        public void apply(DynamicRealmObject obj) {
//                            String fromPhone = Helper.addPlus(obj.getString("from"));
//                            String toPhone = Helper.addPlus(obj.getString("to"));
//                            obj.set("from", fromPhone);
//                            obj.set("to", toPhone);
//                        }
//                    });
//            oldVersion++;
//        }
    }

    @Override
    public int hashCode() {
        return 37;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RealmMigration;
    }
}
