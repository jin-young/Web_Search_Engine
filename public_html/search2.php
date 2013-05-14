<html>
<head>
<meta charset="utf-8">

<script type="text/javascript"
	src="http://ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js"></script>
<script type="text/javascript"
	src="http://ajax.aspnetcdn.com/ajax/jquery.validate/1.11.0/jquery.validate.js"></script>
<script type="text/javascript" src="./scripts/g04.js"></script>
<link rel="stylesheet" href="./styles/g04.css" type="text/css" media="screen" />

<?php 
  $v = $_GET["query"];
  if($v) {
    $query = str_replace(' ', '%20', $v);
  	$query = str_replace('"', '%22', $query);
  	$json_url = 'http://localhost:25804/search?query=' . $query . '&format=json&ranker=favorite';
     
    // Getting results
    $result =  file_get_contents($json_url); // Getting jSON result string
    $json_output = json_decode($result, true);
  } else {

  }
?>

</head>	
<body>
	<div>
		
	  <form acton="search.php" method="get" onsubmit="return isQueryNotEmpty(this);">
	    <a href="index.html"> 
		    <img align="absmiddle" width="100" height="30" src="./image/google.png"> 
	    </a>
		  <input type="text" name="query" style="width: 600; height:30" value="<?php echo $_GET["query"] ?>"/> 
		  <input type="submit" id="search-submit" value=""
		        style="vertical-align: top; background-image:url(image/searchbutton.png); border: solid 0px #000000; width: 70; height: 30;" />
	  </form>
	</div>
	<hr />
  <div id="main" style="padding: 10px; margin-left: 10px">
    <div id="peformance_summarny" style="margin-bottom: 5px">
      <?php 
        if($v) {
          echo $json_output["_num_of_result"] . " results (" . $json_output["_run_time"] . " seconds)";
        }
      ?>
    </div>
    <div id="result_list" style="width: 60%; float: left">
	    <!-- convert Json into html format -->
	    <?php
	      if($v) {
	        echo "Showing results for <b>" . $v . "</b><br/><br/>";
	        $j=0;
	        foreach ( $json_output["_sDocs"] as $record ) {
      ?>
        <div style="margin-bottom: 10px">
     <?php 	  echo '<form id="f'.$j.'" method="POST" action="log.php"><input type="hidden" name="url" value="'.$record['_doc']['_url'].'" /><input type="hidden" name="query" value="'.$v.'" /></form>
				<font size="4em"><a href=" " onclick="document.forms.f'.$j.'.submit();">'.$record["_doc"]["_title"].'</a></font><br/>';
$j++;  ?>  
          <font style="color:green"><?php echo $record["_doc"]["_url"]; ?></font><br/>
          <?php
            foreach ( $record["_doc"]["texts2Display"] as $snippet ) {
              echo $snippet;
            }
          ?>
        </div>
      <?php
          }
	      }
	    ?>
	  </div>
	  <div id="ad_list" style="width: 60%; float: left">
	    
	  </div>
  </div>
	<script language="javascript">
	  function isQueryNotEmpty(form) {
	    if(form.query.value == null || form.query.value == undefined || form.query.value.trim() == "") {
  	    return false;
	    }
	    return true;
	  }	 
	</script>
</body>
</html>
