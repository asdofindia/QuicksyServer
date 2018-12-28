<#ftl output_format="HTML">
<#import "enter.ftl" as enter/>
<@enter.enter title="Your Jabber ID has been entered into the Quicksy directory">
<p>Quicksy users are now able to discover your Jabber ID <strong>${jid}</strong> by looking up your phone number <strong>${number}</strong><#if attempts gt 0> (<a href="/enter/confirm-reset/">change</a>)</#if>.</p>
<p>That’s it. Thank you for supporting <a href="https://quicksy.im">Quicksy</a>.</p>

<p class="secondary small">
Click <a href="/enter/confirm-delete/">here</a> if you want to delete the entry. Note that deleting your Jabber ID from the directory is permanent. If you change your mind later you will have to pay the fee again and (re)verify your phone number.
</p>
</@enter.enter>
