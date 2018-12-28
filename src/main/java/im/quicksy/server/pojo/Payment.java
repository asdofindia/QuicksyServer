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

import rocks.xmpp.addr.Jid;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Payment {

    public static final float FEE = 4.99f;

    private UUID uuid;
    private Jid owner;
    private PaymentMethod method;
    private float total;
    private String token;
    private PaymentStatus status;
    private LocalDateTime created;


    public Payment(Jid jid, PaymentMethod method) {
        this.uuid = UUID.randomUUID();
        this.owner = jid;
        this.method = method;
        this.total = method == PaymentMethod.VOUCHER ? 0.0f : FEE;
        this.created = LocalDateTime.now();
        this.status = PaymentStatus.PENDING;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Jid getOwner() {
        return owner;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public float getTotal() {
        return total;
    }

    public String getToken() {
        return token;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public List<ShoppingCartItem> getItems() {
        return Collections.singletonList(new ShoppingCartItem("Quicksy Directory entry", total));
    }

    public void setToken(String token) {
        this.token = token;
    }
}
