<tr>
    <td class="nameCol">
        <div>
            <g:if test="${tip.deprecated}">
                <span class="label label-danger" title="This Trust Profile should no longer be used.  There are likely replacements or newer Trust Profiles available.">Deprecated</span>
            </g:if>
            <tmf:createLink tip="${tip}" format="html">${tip.name}</tmf:createLink>
            <div style="float: right">
                <tmf:createLink tip="${tip}" format="xml">XML</tmf:createLink>
                |
                <tmf:createLink tip="${tip}" format="json">JSON</tmf:createLink>
            </div>
        </div>
        <div class="text-muted" style="margin-left: 0.5em;"><small>${tip.description}</small></div>
    </td>
    <td class="versionCol">${tip.tipVersion}</td>
</tr>