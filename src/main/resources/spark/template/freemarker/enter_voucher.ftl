<#ftl output_format="HTML">
<#import "enter.ftl" as enter/>
<@enter.enter title="Redeem voucher">
<p>Please enter your voucher below.</p>
<form method="post" action="/enter/voucher/">
  <input name="voucher" type="text" placeholder="Voucher" autocomplete="off"/>
  <p class="continue"><button type="submit">Redeem</button></p>
</form>
</@enter.enter>
