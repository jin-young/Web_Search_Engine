<?php
	include_once("../models/dbModel.php");
	
	$request = $_REQUEST["request"];
	$company = $_REQUEST["company"];
	$title = $_REQUEST["title"];
	$url = $_REQUEST["url"];
	$content = $_REQUEST["content"];
	$cost = $_REQUEST["cost"];
	switch($request){
		case "register":
			$result = register($company, $title, $url, $content, $cost);
			echo $result;
			break;
	}

?>