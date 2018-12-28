<#ftl output_format="HTML">
<#import "enter.ftl" as enter/>
<@enter.enter title="Pay registration fee">
<#if error??>
  <p class="error">
    <#switch error>
      <#case "voucher">
      The voucher you have entered is invalid.
      <#break>
      <#case "failed">
      The payment was unsuccessful.
      <#break>
      <#case "redeem-failed">
      Your payment has been received but an internal database error has occurred. Please contact our support.
      <#break>
      <#case "redeem-voucher">
      Unable to redeem voucher. Please contact our support.
      <#break>
      <#case "cart">
      Shopping cart not found. Payment failed.
      <#break>
      <#case "not-yet">
      We haven’t received a payment from you yet.
      <#break>
      <#default>
      An unknown error has occurred.
    </#switch>
  </p>
</#if>
<p>Pay the registration fee of <strong>${fee?string["0.00"]} Euro</strong> using the button below or <a href="/enter/voucher/">redeem a voucher</a>.</p>
<p>The fee will be credited to your Jabber ID: <strong>${jid}</strong>. You won’t be able to change your Jabber ID later.</p>
<p>We will ask you to verify your phone number after we have received the payment.</p>
<form method="post" action="/enter/checkout/">
<p class="checkout"><button type="submit">Pay with PayPal</button></p>
</form>
<p class="small secondary">Click <a href="/enter/send-jabber-verification/">here</a> if you want to start over and use a different Jabberd ID.</a>
</@enter.enter>
