<form action="addadvertising.html" method="post" id="addqform"
		enctype="multipart/form-data">
	
	<table bgcolor="#FFF">
		<tr>
			<td>
				<h2 style="padding-top: 0px; xsmargin-top: 0px;">Add New Advertisement</h2>
			</td>
		</tr>
		<tr>
			<td> 
				<input type="text" id="company" placeholder="Company" size="47"
				   style="background-color:#dfffff;color:gray">
			</td>
		</tr>
		<tr>
			<td> 
				<input type="text" id="title" placeholder="Title" size="47"
					 style="background-color:#dfffff;color:gray">
			</td>
		</tr>
		<tr>
			<td> 
				<input type="text" id="url" placeholder="URL" size="47" 
					 style="background-color:#dfffff;color:gray">
			</td>
		</tr>
		<tr>
			<td> 
				<textarea id="content" rows="10" cols="50" style="background-color:#dfffff;"></textarea>
			</td>
		</tr>
		<tr>
			<td> 
				$&nbsp;<input type="text" id="cost" placeholder="Cost" 
					 style="background-color:#dfffff;color:gray">
			</td>
		</tr>
		<tr>
			<td align="center"> <!-- the argument should check -->
				<button type="button" onClick="alert($('#title').attr('value')); register($('#company').attr('value'), 
														$('#title').attr('value'),
														$('#url').attr('value'),
														$('#content').attr('value'),
														$('#cost').attr('value'))">
				Register</button>
				&nbsp;&nbsp;&nbsp;&nbsp;
				<button type="button" onClick="clearAll();">Cancel</button>
			</td>
	</table>
</form>
