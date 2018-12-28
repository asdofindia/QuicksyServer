/*
 * Copyright 2018 Daniel Gultsch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.quicksy.server.pojo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import im.quicksy.server.Configuration;
import im.quicksy.server.controller.BaseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Voucher {

    private static final Logger LOGGER = LoggerFactory.getLogger(Voucher.class);

    private String code;
    private Date from;
    private Date to;

    public boolean isCurrentlyValid() {
        final Date now = Date.from(Instant.now());
        return from != null && to != null && from.before(now) && to.after(now);
    }

    public static boolean checkVoucher(String code) {
        try {
            final Gson gson = new GsonBuilder().create();
            List<Voucher> vouchers = gson.fromJson(new FileReader(Configuration.getInstance().getVoucherFile()),new TypeToken<List<Voucher>>(){}.getType());
            LOGGER.info(vouchers.size()+" vouchers on file");
            for(Voucher voucher : vouchers) {
                if (code.equals(voucher.code) && voucher.isCurrentlyValid()) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
