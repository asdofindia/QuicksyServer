<#ftl output_format="HTML">
<#import "enter.ftl" as enter/>
<@enter.enter title="Enter your Jabber ID">
<p>Please enter your Jabber ID. We will send you a message with a verification code.</p>
<#if error??>
  <p class="error">
    <#switch error>
      <#case "cookies">
      You need to enable session cookies.
      <#break>
      <#case "invalid">
      The Jabber ID you entered is invalid.
      <#break>
      <#case "no-quicksy">
      You can not enter a quicksy.im Jabber ID into the directory
      <#break>
      <#case "attempts">
      Too many failed attempts.
      <#break>
      <#default>
      An unknown error has occurred.
    </#switch>
  </p>
</#if>
<form method="post" action="/enter/send-jabber-verification/">
  <div class="input-wrapper">
    <input name="jid" type="text" placeholder="username@example.com" autocomplete="off"/>
  </div>
  <p class="continue"><button type="submit">Next</button></p>
</form>
<p class="secondary small">The verification message will be sent from the Jabber ID <em>quicksy.im</em>. If you have problems receiving that message make sure your server is not blocking messages from strangers. It might help to temporarily add <em>quicksy.im</em> as a contact to your roster.</p>
</@enter.enter>
